package io.forest.lakehouse.onboarding;

import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.CatalogUtil;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DataFiles;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.HasTableOperations;
import org.apache.iceberg.OverwriteFiles;
import org.apache.iceberg.PartitionKey;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.SnapshotRef;
import org.apache.iceberg.Table;
import org.apache.iceberg.TableMetadata;
import org.apache.iceberg.TableProperties;
import org.apache.iceberg.TableScan;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.Namespace;
import org.apache.iceberg.catalog.SupportsNamespaces;
import org.apache.iceberg.catalog.TableCommit;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.data.parquet.GenericParquetReaders;
import org.apache.iceberg.data.parquet.GenericParquetWriter;
import org.apache.iceberg.exceptions.CommitFailedException;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.FileAppender;
import org.apache.iceberg.io.OutputFile;
import org.apache.iceberg.parquet.Parquet;
import org.apache.iceberg.rest.RESTCatalog;
import org.apache.iceberg.types.Types;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;

import static org.apache.iceberg.types.Types.NestedField.required;

/**
 * {@link OnboardingStore} (and {@link OnboardingLakehouse}) over Apache Iceberg.
 *
 * <p><strong>Cross-table atomicity comes from the catalog, not from ordering commits.</strong> The
 * unit of work buffers every row and publishes it as one {@link TableCommit} per touched table,
 * handed to the catalog in a single {@code commitTransaction} call. That call is the whole point:
 * Iceberg added it in 1.8 and exposes it only on the REST client
 * ({@code RESTCatalog#commitTransaction}), over the REST protocol's
 * {@code POST /v1/{prefix}/transactions/commit} -- see ADR-009. The catalog applies all of the
 * commits or none of them, so a writer that dies between the party write and the contact-point write
 * leaves nothing behind (INV-2), and no application-side ordering has to be trusted.
 *
 * <p><strong>Every {@link TableCommit} asserts the snapshot the transaction read.</strong>
 * {@code TableCommit.create} derives an {@code AssertRefSnapshotID} requirement per table, so a
 * concurrent writer on <em>any</em> of the four tables is refused rather than quietly merged on top
 * of. That is stricter than the optimistic merge the plain {@code table.newAppend()} path performs.
 *
 * <p><strong>Copy-on-write, no merge-on-read.</strong> A row is updated by writing a new file and
 * naming the exact file that was read, so reading a snapshot's live data files <em>is</em> reading
 * the snapshot's contents.
 */
public final class IcebergOnboardingStore implements OnboardingStore, OnboardingLakehouse {

    private static final String PARTY_TABLE = "party";
    private static final String RELATIONSHIP_TABLE = "party_relationship";
    private static final String CONTACT_POINT_TABLE = "contact_point";
    private static final String COUNTER_TABLE = "party_counter";

    private static final Schema PARTY_SCHEMA = new Schema(
            required(1, "party_id", Types.StringType.get()),
            required(2, "party_type", Types.StringType.get()),
            required(3, "legal_name", Types.StringType.get()),
            required(4, "tax_id", Types.StringType.get()),
            required(5, "kyc_level", Types.StringType.get()));

    private static final Schema RELATIONSHIP_SCHEMA = new Schema(
            required(1, "relationship_id", Types.StringType.get()),
            required(2, "party_id", Types.StringType.get()),
            required(3, "relationship_type", Types.StringType.get()),
            required(4, "status", Types.StringType.get()),
            required(5, "effective_from", Types.DateType.get()));

    private static final Schema CONTACT_POINT_SCHEMA = new Schema(
            required(1, "contact_point_id", Types.StringType.get()),
            required(2, "party_id", Types.StringType.get()),
            required(3, "channel", Types.StringType.get()),
            required(4, "value", Types.StringType.get()),
            required(5, "primary", Types.BooleanType.get()));

    private static final Schema COUNTER_SCHEMA = new Schema(
            required(1, "party_id", Types.StringType.get()),
            required(2, "seq", Types.IntegerType.get()));

    private final Catalog catalog;
    private final MultiTableCommit committer;
    private final Map<OnboardingTable, TableIdentifier> identifiers;

    public IcebergOnboardingStore(Catalog catalog, MultiTableCommit committer, String namespace) {
        this.catalog = catalog;
        this.committer = committer;
        Namespace ns = Namespace.of(namespace);
        Map<OnboardingTable, TableIdentifier> ids = new EnumMap<>(OnboardingTable.class);
        ids.put(OnboardingTable.PARTY, TableIdentifier.of(ns, PARTY_TABLE));
        ids.put(OnboardingTable.PARTY_RELATIONSHIP, TableIdentifier.of(ns, RELATIONSHIP_TABLE));
        ids.put(OnboardingTable.CONTACT_POINT, TableIdentifier.of(ns, CONTACT_POINT_TABLE));
        ids.put(OnboardingTable.PARTY_COUNTER, TableIdentifier.of(ns, COUNTER_TABLE));
        this.identifiers = Map.copyOf(ids);
    }

    /**
     * The one catalog capability this adapter cannot work without. It is a type rather than a comment
     * because a catalog that cannot publish several tables in one action cannot honour INV-2, and
     * silently degrading to "commit them one after another" is the design ADR-009 rejects.
     */
    @FunctionalInterface
    public interface MultiTableCommit {

        void commitTransaction(List<TableCommit> commits);
    }

    /**
     * Connects to a REST catalog -- the only Iceberg client that exposes the multi-table commit
     * (ADR-009) -- and creates the namespace on first use.
     */
    public static IcebergOnboardingStore open(Map<String, String> catalogProperties, String namespace) {
        Catalog catalog = CatalogUtil.loadCatalog(RESTCatalog.class.getName(), "ltap", catalogProperties, null);
        if (catalog instanceof SupportsNamespaces namespaces
                && !namespaces.namespaceExists(Namespace.of(namespace))) {
            namespaces.createNamespace(Namespace.of(namespace));
        }
        if (!(catalog instanceof RESTCatalog rest)) {
            throw new IllegalStateException("cross-table atomicity needs the REST catalog's multi-table "
                    + "commit, but the loaded catalog is " + catalog.getClass().getName());
        }
        return new IcebergOnboardingStore(catalog, rest::commitTransaction, namespace);
    }

    public void close() {
        if (catalog instanceof java.io.Closeable closeable) {
            try {
                closeable.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /** Creates any of the four tables that is not there yet. */
    public void createTables() {
        createIfAbsent(OnboardingTable.PARTY, PARTY_SCHEMA, partitionSpec(PARTY_SCHEMA, "party_id"), Map.of());
        createIfAbsent(OnboardingTable.PARTY_RELATIONSHIP, RELATIONSHIP_SCHEMA,
                partitionSpec(RELATIONSHIP_SCHEMA, "party_id"), Map.of());
        // Partitioned by contact_point_id, not by party_id: two demotions of *different* contact
        // points of one party have to be able to replace *different* partitions, or the write skew
        // INV-1 is about could not be reproduced at all. The shared serialization point is the
        // counter table, which is partitioned by party_id.
        createIfAbsent(OnboardingTable.CONTACT_POINT, CONTACT_POINT_SCHEMA,
                partitionSpec(CONTACT_POINT_SCHEMA, "contact_point_id"), Map.of());
        // Automatic commit retry is switched off so a moved base snapshot is *refused* rather than
        // transparently re-applied on top of the winner -- the compare-and-set behaviour ADR-003
        // records for the counter table.
        createIfAbsent(OnboardingTable.PARTY_COUNTER, COUNTER_SCHEMA,
                partitionSpec(COUNTER_SCHEMA, "party_id"), Map.of(TableProperties.COMMIT_NUM_RETRIES, "0"));
    }

    /** Drops and recreates the four tables, so a scenario starts from one empty snapshot. */
    public void reset() {
        for (TableIdentifier id : identifiers.values()) {
            if (catalog.tableExists(id)) {
                catalog.dropTable(id, true);
            }
        }
        createTables();
    }

    private void createIfAbsent(OnboardingTable type, Schema schema, PartitionSpec spec,
                                Map<String, String> properties) {
        TableIdentifier id = identifiers.get(type);
        if (!catalog.tableExists(id)) {
            catalog.createTable(id, schema, spec, properties);
        }
    }

    private static PartitionSpec partitionSpec(Schema schema, String column) {
        return PartitionSpec.builderFor(schema).identity(column).build();
    }

    // ------------------------------------------------------------------ OnboardingStore

    /**
     * A unit of work the caller commits itself. {@link #inTransaction} is exactly this plus one
     * commit, and is what application code should use; this exists so a scenario can hold two
     * writers open at once and interleave them deliberately instead of racing them.
     *
     * <p>Abandoning one publishes nothing: an uncommitted unit of work has no side effects to undo.
     */
    public interface UnitOfWork extends OnboardingTx {

        void commit();
    }

    /**
     * Opens a unit of work the caller commits. Every read in it is pinned to the snapshot the
     * caller's first read saw, so two of these opened before either commits genuinely overlap.
     */
    public UnitOfWork begin() {
        return new IcebergTx();
    }

    @Override
    public <T> T inTransaction(Function<OnboardingTx, T> action) {
        UnitOfWork tx = begin();
        T result = action.apply(tx);
        tx.commit();
        return result;
    }

    @Override
    public Optional<Party> findParty(String partyId) {
        return partyAt(currentSnapshotId(OnboardingTable.PARTY), partyId);
    }

    @Override
    public List<PartyRelationship> relationshipsOf(String partyId) {
        return relationshipsAt(currentSnapshotId(OnboardingTable.PARTY_RELATIONSHIP), partyId);
    }

    @Override
    public List<ContactPoint> contactPointsOf(String partyId) {
        return contactPointsAt(currentSnapshotId(OnboardingTable.CONTACT_POINT), partyId);
    }

    // ------------------------------------------------------------------ OnboardingLakehouse

    @Override
    public long currentSnapshotId(OnboardingTable type) {
        Table table = table(type);
        return table.currentSnapshot() == null ? -1L : table.currentSnapshot().snapshotId();
    }

    @Override
    public Map<String, Long> customersByRelationshipTypeAsOf(long snapshotId) {
        Table table = table(OnboardingTable.PARTY_RELATIONSHIP);
        Map<String, Long> counts = new TreeMap<>();
        for (Row row : readRows(table, snapshotId, table.schema().select("relationship_type", "status"),
                Expressions.equal("status", RelationshipStatus.ACTIVE.name()))) {
            if (RelationshipStatus.ACTIVE.name().equals(row.record().getField("status"))) {
                counts.merge((String) row.record().getField("relationship_type"), 1L, Long::sum);
            }
        }
        return counts;
    }

    @Override
    public Map<String, Long> customersByRelationshipType() {
        return customersByRelationshipTypeAsOf(currentSnapshotId(OnboardingTable.PARTY_RELATIONSHIP));
    }

    @Override
    public int dataFileCount(OnboardingTable type) {
        int files = 0;
        try (CloseableIterable<FileScanTask> tasks = table(type).newScan().planFiles()) {
            for (FileScanTask ignored : tasks) {
                files++;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return files;
    }

    @Override
    public int counter(String partyId) {
        Table table = table(OnboardingTable.PARTY_COUNTER);
        for (Row row : readRows(table, currentSnapshotId(OnboardingTable.PARTY_COUNTER),
                table.schema().select("party_id", "seq"), Expressions.equal("party_id", partyId))) {
            if (partyId.equals(row.record().getField("party_id"))) {
                return (Integer) row.record().getField("seq");
            }
        }
        return -1;
    }

    @Override
    public String renderState() {
        return "party[" + render(OnboardingTable.PARTY, "party_id", "legal_name")
                + "] party_relationship[" + render(OnboardingTable.PARTY_RELATIONSHIP, "relationship_id", "status")
                + "] contact_point[" + render(OnboardingTable.CONTACT_POINT, "contact_point_id", "primary")
                + "] party_counter[" + render(OnboardingTable.PARTY_COUNTER, "party_id", "seq") + "]";
    }

    /**
     * Renders one snapshot's rows as sorted {@code key=value} pairs. The pair is rendered on one line
     * deliberately: rendering the key column and the value column separately and sorting each would
     * pair a key with somebody else's value as soon as the two orders differ.
     */
    private String render(OnboardingTable type, String keyColumn, String valueColumn) {
        Table table = table(type);
        List<String> rows = new ArrayList<>();
        for (Row row : readRows(table, currentSnapshotId(type),
                table.schema().select(keyColumn, valueColumn), null)) {
            rows.add(row.record().getField(keyColumn) + "=" + row.record().getField(valueColumn));
        }
        rows.sort(String::compareTo);
        return String.join(" ", rows);
    }

    private Table table(OnboardingTable type) {
        return catalog.loadTable(identifiers.get(type));
    }

    // ------------------------------------------------------------------ the unit of work

    /**
     * Buffers one unit of work against the snapshot its reads came from.
     *
     * <p>Two things make this a snapshot-isolated transaction rather than a bag of writes. First, the
     * table handles and their metadata are captured once, at the start, so every read -- and the
     * {@link TableCommit} built at the end -- describes the same base snapshot. Second, nothing is
     * published until {@link #commit()} runs, and then all of it is published in one catalog action.
     */
    private final class IcebergTx implements UnitOfWork {

        private final Map<OnboardingTable, Table> tables = new EnumMap<>(OnboardingTable.class);
        private final Map<OnboardingTable, TableMetadata> bases = new EnumMap<>(OnboardingTable.class);
        private final Map<OnboardingTable, List<Record>> inserts = new EnumMap<>(OnboardingTable.class);
        /** New version of a row, keyed by the row's identity, plus the file that version replaces. */
        private final Map<OnboardingTable, Map<String, Replacement>> replacements =
                new EnumMap<>(OnboardingTable.class);
        private final Set<String> touchedParties = new LinkedHashSet<>();
        private boolean committed;

        private IcebergTx() {
            for (OnboardingTable type : OnboardingTable.values()) {
                Table table = catalog.loadTable(identifiers.get(type));
                tables.put(type, table);
                bases.put(type, ((HasTableOperations) table).operations().current());
            }
        }

        private long baseSnapshot(OnboardingTable type) {
            TableMetadata base = bases.get(type);
            return base.currentSnapshot() == null ? -1L : base.currentSnapshot().snapshotId();
        }

        /** The rows this unit of work has staged for one key, whether inserts or new versions. */
        private List<Record> staged(OnboardingTable type, String identityColumn, String identity) {
            List<Record> rows = new ArrayList<>();
            for (Record record : inserts.getOrDefault(type, List.of())) {
                if (identity.equals(record.getField(identityColumn))) {
                    rows.add(record);
                }
            }
            Replacement replacement = replacements.getOrDefault(type, Map.of()).get(identity);
            if (replacement != null) {
                rows.add(replacement.record());
            }
            return rows;
        }

        @Override
        public Optional<Party> findParty(String partyId) {
            List<Record> staged = staged(OnboardingTable.PARTY, "party_id", partyId);
            return staged.isEmpty()
                    ? partyAt(baseSnapshot(OnboardingTable.PARTY), partyId)
                    : Optional.of(toParty(staged.get(0)));
        }

        @Override
        public Optional<Party> findPartyByTaxId(String taxId) {
            for (Record record : inserts.getOrDefault(OnboardingTable.PARTY, List.of())) {
                if (taxId.equals(record.getField("tax_id"))) {
                    return Optional.of(toParty(record));
                }
            }
            Table table = tables.get(OnboardingTable.PARTY);
            for (Row row : readRows(table, baseSnapshot(OnboardingTable.PARTY), table.schema(),
                    Expressions.equal("tax_id", taxId))) {
                if (taxId.equals(row.record().getField("tax_id"))) {
                    return Optional.of(toParty(row.record()));
                }
            }
            return Optional.empty();
        }

        @Override
        public List<ContactPoint> contactPointsOf(String partyId) {
            // Committed rows first, then this unit of work's own writes on top of them, so the count
            // INV-1 is expressed against is the one the commit will actually be judged on.
            Map<String, ContactPoint> contactPoints = new LinkedHashMap<>();
            for (ContactPoint contactPoint : contactPointsAt(baseSnapshot(OnboardingTable.CONTACT_POINT), partyId)) {
                contactPoints.put(contactPoint.contactPointId(), contactPoint);
            }
            for (Record record : inserts.getOrDefault(OnboardingTable.CONTACT_POINT, List.of())) {
                ContactPoint contactPoint = toContactPoint(record);
                if (contactPoint.partyId().equals(partyId)) {
                    contactPoints.put(contactPoint.contactPointId(), contactPoint);
                }
            }
            for (Map.Entry<String, Replacement> entry
                    : replacements.getOrDefault(OnboardingTable.CONTACT_POINT, Map.of()).entrySet()) {
                ContactPoint contactPoint = toContactPoint(entry.getValue().record());
                if (contactPoint.partyId().equals(partyId)) {
                    contactPoints.put(entry.getKey(), contactPoint);
                }
            }
            return List.copyOf(contactPoints.values());
        }

        @Override
        public boolean contactPointExists(ContactChannel channel, String value) {
            for (Record record : inserts.getOrDefault(OnboardingTable.CONTACT_POINT, List.of())) {
                if (channel.name().equals(record.getField("channel")) && value.equals(record.getField("value"))) {
                    return true;
                }
            }
            Table table = tables.get(OnboardingTable.CONTACT_POINT);
            for (Row row : readRows(table, baseSnapshot(OnboardingTable.CONTACT_POINT), table.schema(),
                    Expressions.and(Expressions.equal("channel", channel.name()),
                            Expressions.equal("value", value)))) {
                if (channel.name().equals(row.record().getField("channel"))
                        && value.equals(row.record().getField("value"))) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void createParty(Party party) {
            insert(OnboardingTable.PARTY, partyRecord(party));
        }

        @Override
        public void createRelationship(PartyRelationship relationship) {
            insert(OnboardingTable.PARTY_RELATIONSHIP, relationshipRecord(relationship));
        }

        @Override
        public void createContactPoint(ContactPoint contactPoint) {
            insert(OnboardingTable.CONTACT_POINT, contactPointRecord(contactPoint));
        }

        @Override
        public void setPrimary(String partyId, String contactPointId, boolean primary) {
            for (Record record : inserts.getOrDefault(OnboardingTable.CONTACT_POINT, List.of())) {
                if (contactPointId.equals(record.getField("contact_point_id"))) {
                    record.setField("primary", primary);      // this row is not committed yet
                    return;
                }
            }
            Table table = tables.get(OnboardingTable.CONTACT_POINT);
            for (Row row : readRows(table, baseSnapshot(OnboardingTable.CONTACT_POINT), table.schema(),
                    Expressions.equal("contact_point_id", contactPointId))) {
                if (contactPointId.equals(row.record().getField("contact_point_id"))) {
                    ContactPoint updated = toContactPoint(row.record()).withPrimary(primary);
                    // Deliberately names the file this version was read from: that is what makes the
                    // replacement a compare-and-set instead of a blind overwrite.
                    replace(OnboardingTable.CONTACT_POINT, contactPointId,
                            new Replacement(contactPointRecord(updated), row.file()));
                    touchedParties.add(partyId);
                    return;
                }
            }
            throw new IllegalArgumentException("no contact point " + contactPointId + " on party " + partyId);
        }

        @Override
        public void touch(String partyId) {
            touchedParties.add(partyId);
            Table table = tables.get(OnboardingTable.PARTY_COUNTER);
            for (Row row : readRows(table, baseSnapshot(OnboardingTable.PARTY_COUNTER), table.schema(),
                    Expressions.equal("party_id", partyId))) {
                if (partyId.equals(row.record().getField("party_id"))) {
                    int next = (Integer) row.record().getField("seq") + 1;
                    replace(OnboardingTable.PARTY_COUNTER, partyId,
                            new Replacement(counterRecord(partyId, next), row.file()));
                    return;
                }
            }
            // No counter row yet, so this write is what creates the party's serialization point.
            replace(OnboardingTable.PARTY_COUNTER, partyId,
                    new Replacement(counterRecord(partyId, 0), null));
        }

        /**
         * Publishes everything this unit of work wrote as ONE catalog action.
         *
         * <p>The loop carries no ordering meaning: the catalog receives the whole list and applies
         * all of the commits or none of them. That is what makes a writer dying mid-onboarding
         * unobservable, and it is why no part of this class may fall back to committing the tables
         * one after another.
         */
        public void commit() {
            if (committed) {
                throw new IllegalStateException("this unit of work has already been committed");
            }
            committed = true;
            List<TableCommit> commits = new ArrayList<>();
            for (OnboardingTable type : OnboardingTable.values()) {
                TableCommit tableCommit = stage(type);
                if (tableCommit != null) {
                    commits.add(tableCommit);
                }
            }
            if (commits.isEmpty()) {
                return;
            }
            try {
                committer.commitTransaction(commits);
            } catch (CommitFailedException refused) {
                // Every table's requirement was checked and nothing at all was applied.
                throw new ConcurrentCommitException(String.join(",", touchedParties), refused);
            }
        }

        /** Builds the {@link TableCommit} for one table, staged against the snapshot this tx read. */
        private TableCommit stage(OnboardingTable type) {
            List<Record> newRows = inserts.getOrDefault(type, List.of());
            Collection<Replacement> replacedRows = replacements.getOrDefault(type, Map.of()).values();
            if (newRows.isEmpty() && replacedRows.isEmpty()) {
                return null;
            }
            Table table = tables.get(type);
            TableMetadata base = bases.get(type);
            Snapshot staged;
            if (replacedRows.stream().anyMatch(row -> row.replaced() != null)) {
                // One snapshot carrying the updates and the inserts together: each updated row's
                // previous file is named, each new row arrives as its own file, and the write stays
                // copy-on-write so no merge-on-read is needed to read the snapshot back.
                OverwriteFiles overwrite = table.newOverwrite().stageOnly();
                for (Record record : newRows) {
                    overwrite.addFile(writeFile(table, record));
                }
                for (Replacement row : replacedRows) {
                    overwrite.addFile(writeFile(table, row.record()));
                    if (row.replaced() != null) {
                        overwrite.deleteFile(row.replaced());
                    }
                }
                staged = overwrite.apply();
            } else {
                AppendFiles append = table.newAppend().stageOnly();
                for (Record record : newRows) {
                    append.appendFile(writeFile(table, record));
                }
                for (Replacement row : replacedRows) {
                    append.appendFile(writeFile(table, row.record()));
                }
                staged = append.apply();
            }
            TableMetadata updated = TableMetadata.buildFrom(base)
                    .addSnapshot(staged)
                    .setBranchSnapshot(staged.snapshotId(), SnapshotRef.MAIN_BRANCH)
                    .build();
            return TableCommit.create(identifiers.get(type), base, updated);
        }

        private void insert(OnboardingTable type, Record record) {
            inserts.computeIfAbsent(type, key -> new ArrayList<>()).add(record);
        }

        private void replace(OnboardingTable type, String identity, Replacement replacement) {
            replacements.computeIfAbsent(type, key -> new LinkedHashMap<>()).put(identity, replacement);
        }
    }

    /** A row's new version, plus the exact file held by the version it supersedes (null if new). */
    private record Replacement(Record record, DataFile replaced) {
    }

    // ------------------------------------------------------------------ reading rows

    private Optional<Party> partyAt(long snapshotId, String partyId) {
        Table table = table(OnboardingTable.PARTY);
        for (Row row : readRows(table, snapshotId, table.schema(), Expressions.equal("party_id", partyId))) {
            if (partyId.equals(row.record().getField("party_id"))) {
                return Optional.of(toParty(row.record()));
            }
        }
        return Optional.empty();
    }

    private List<PartyRelationship> relationshipsAt(long snapshotId, String partyId) {
        Table table = table(OnboardingTable.PARTY_RELATIONSHIP);
        List<PartyRelationship> relationships = new ArrayList<>();
        for (Row row : readRows(table, snapshotId, table.schema(), Expressions.equal("party_id", partyId))) {
            if (partyId.equals(row.record().getField("party_id"))) {
                relationships.add(toRelationship(row.record()));
            }
        }
        return List.copyOf(relationships);
    }

    private List<ContactPoint> contactPointsAt(long snapshotId, String partyId) {
        Table table = table(OnboardingTable.CONTACT_POINT);
        List<ContactPoint> contactPoints = new ArrayList<>();
        for (Row row : readRows(table, snapshotId, table.schema(), Expressions.equal("party_id", partyId))) {
            if (partyId.equals(row.record().getField("party_id"))) {
                contactPoints.add(toContactPoint(row.record()));
            }
        }
        return List.copyOf(contactPoints);
    }

    // ------------------------------------------------------------------ row mapping

    private static Party toParty(Record record) {
        return new Party((String) record.getField("party_id"),
                PartyType.valueOf((String) record.getField("party_type")),
                (String) record.getField("legal_name"),
                (String) record.getField("tax_id"),
                KycLevel.valueOf((String) record.getField("kyc_level")));
    }

    private static PartyRelationship toRelationship(Record record) {
        return new PartyRelationship((String) record.getField("relationship_id"),
                (String) record.getField("party_id"),
                RelationshipType.valueOf((String) record.getField("relationship_type")),
                RelationshipStatus.valueOf((String) record.getField("status")),
                (LocalDate) record.getField("effective_from"));
    }

    private static ContactPoint toContactPoint(Record record) {
        return new ContactPoint((String) record.getField("contact_point_id"),
                (String) record.getField("party_id"),
                ContactChannel.valueOf((String) record.getField("channel")),
                (String) record.getField("value"),
                Boolean.TRUE.equals(record.getField("primary")));
    }

    private static Record partyRecord(Party party) {
        Record record = GenericRecord.create(PARTY_SCHEMA);
        record.setField("party_id", party.partyId());
        record.setField("party_type", party.type().name());
        record.setField("legal_name", party.legalName());
        record.setField("tax_id", party.taxId());
        record.setField("kyc_level", party.kycLevel().name());
        return record;
    }

    private static Record relationshipRecord(PartyRelationship relationship) {
        Record record = GenericRecord.create(RELATIONSHIP_SCHEMA);
        record.setField("relationship_id", relationship.relationshipId());
        record.setField("party_id", relationship.partyId());
        record.setField("relationship_type", relationship.type().name());
        record.setField("status", relationship.status().name());
        record.setField("effective_from", relationship.effectiveFrom());
        return record;
    }

    private static Record contactPointRecord(ContactPoint contactPoint) {
        Record record = GenericRecord.create(CONTACT_POINT_SCHEMA);
        record.setField("contact_point_id", contactPoint.contactPointId());
        record.setField("party_id", contactPoint.partyId());
        record.setField("channel", contactPoint.channel().name());
        record.setField("value", contactPoint.value());
        record.setField("primary", contactPoint.primary());
        return record;
    }

    private static Record counterRecord(String partyId, int seq) {
        Record record = GenericRecord.create(COUNTER_SCHEMA);
        record.setField("party_id", partyId);
        record.setField("seq", seq);
        return record;
    }

    // ------------------------------------------------------------------ files

    /**
     * Writes one row as a Parquet file and describes it as a {@link DataFile} Iceberg can commit.
     *
     * <p>The length is read from the closed object, never from the appender: the Parquet footer is
     * only written on close, so a length captured earlier puts a {@code file_size_in_bytes} in the
     * manifest that no engine trusting the manifest can read back.
     */
    private static DataFile writeFile(Table table, Record record) {
        Schema schema = table.schema();
        String path = table.location() + "/data/" + UUID.randomUUID() + ".parquet";
        OutputFile output = table.io().newOutputFile(path);
        try (FileAppender<Record> appender = Parquet.write(output)
                .schema(schema)
                .createWriterFunc(GenericParquetWriter::create)
                .build()) {
            appender.add(record);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write " + path, e);
        }
        long length = output.toInputFile().getLength();
        PartitionKey partition = new PartitionKey(table.spec(), schema);
        partition.partition(record);
        return DataFiles.builder(table.spec())
                .withPath(path)
                .withFormat(FileFormat.PARQUET)
                .withFileSizeInBytes(length)
                .withRecordCount(1)
                .withPartition(partition)
                .build();
    }

    /** One row of one snapshot, together with the exact data file it was read out of. */
    private record Row(DataFile file, Record record) {
    }

    /**
     * Reads the rows of one snapshot, projecting columns and pushing the predicate into the scan so
     * Iceberg can prune manifests and files before a byte of data is fetched. Every write here is a
     * copy-on-write replace, so reading the live data files <em>is</em> reading the snapshot.
     *
     * <p>The predicate is re-applied in memory rather than trusted to the reader: these tables are
     * tiny, and it keeps the evidence independent of file-level statistics.
     */
    private static List<Row> readRows(Table table, long snapshotId, Schema projection, Expression predicate) {
        if (snapshotId < 0) {
            return List.of();               // the table has never been committed to
        }
        TableScan scan = table.newScan().useSnapshot(snapshotId);
        if (projection != null) {
            scan = scan.project(projection);
        }
        if (predicate != null) {
            scan = scan.filter(predicate);
        }
        List<Row> rows = new ArrayList<>();
        try (CloseableIterable<FileScanTask> tasks = scan.planFiles()) {
            for (FileScanTask task : tasks) {
                Schema taskSchema = task.schema();
                try (CloseableIterable<Record> records = Parquet.read(
                                table.io().newInputFile(task.file().path().toString()))
                        .project(taskSchema)
                        .createReaderFunc(messageType -> GenericParquetReaders.buildReader(taskSchema, messageType))
                        .build()) {
                    for (Record record : records) {
                        rows.add(new Row(task.file(), record));
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return rows;
    }
}
