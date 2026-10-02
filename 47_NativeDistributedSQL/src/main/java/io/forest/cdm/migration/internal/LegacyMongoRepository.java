package io.forest.cdm.migration.internal;

import io.forest.cdm.migration.LegacyCustomer;
import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoChangeStreamCursor;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import org.bson.BsonDocument;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Module-internal access to the legacy MongoDB store.
 *
 * <p>The client is created lazily so the rest of the application starts even when the legacy store
 * is not deployed (true for every proof of concept except migration).
 *
 * <p>The change-stream resume token is persisted in a {@code migration_state} collection so a
 * reconcile pass resumes where the previous one stopped rather than missing changes in between.
 */
@Repository
public class LegacyMongoRepository implements LegacyCustomerStore {

    private static final String STATE_COLLECTION = "migration_state";
    private static final String TOKEN_ID = "resumeToken";

    private final String uri;
    private volatile MongoClient client;
    private BsonDocument lastResumeToken;

    public LegacyMongoRepository(
            @Value("${cdm.migration.mongo-uri:mongodb://localhost:27017/?replicaSet=rs0"
                    + "&serverSelectionTimeoutMS=3000&connectTimeoutMS=3000}") String uri) {
        this.uri = uri;
    }

    /** Inserts or replaces a legacy customer, keyed by legal name. */
    public void upsert(LegacyCustomer customer) {
        customers().replaceOne(
                Filters.eq("legalName", customer.legalName()),
                toDocument(customer),
                new ReplaceOptions().upsert(true));
    }

    public List<LegacyCustomer> findAll() {
        List<LegacyCustomer> customers = new ArrayList<>();
        for (Document document : customers().find()) {
            customers.add(fromDocument(document));
        }
        return customers;
    }

    /**
     * Drains changes recorded since the persisted resume token.
     *
     * <p>Returns the changed documents only; applying them to the CDM store is the service's job.
     *
     * @throws IllegalStateException if the change stream cannot be opened at all, which the caller
     *         treats as a signal to fall back to a full sweep
     */
    public List<LegacyCustomer> drainChangedCustomers(Duration maxAwait) {
        List<LegacyCustomer> changed = new ArrayList<>();
        BsonDocument resumeFrom = resumeToken();
        ChangeStreamIterable<Document> stream = customers().watch();
        if (resumeFrom != null) {
            stream = stream.resumeAfter(resumeFrom);
        }
        stream = stream.fullDocument(FullDocument.UPDATE_LOOKUP)
                .maxAwaitTime(maxAwait.toMillis(), TimeUnit.MILLISECONDS);

        try (MongoChangeStreamCursor<ChangeStreamDocument<Document>> cursor = stream.cursor()) {
            while (cursor.hasNext()) {
                ChangeStreamDocument<Document> change = cursor.next();
                Document full = change.getFullDocument();
                if (full != null) {
                    changed.add(fromDocument(full));
                }
                lastResumeToken = cursor.getResumeToken();
            }
        }
        saveResumeToken();
        return changed;
    }

    // ------------------------------------------------------------------ internals

    private MongoCollection<Document> customers() {
        return client().getDatabase("legacy").getCollection("customers");
    }

    private MongoCollection<Document> state() {
        return client().getDatabase("legacy").getCollection(STATE_COLLECTION);
    }

    private MongoClient client() {
        MongoClient current = client;
        if (current == null) {
            synchronized (this) {
                current = client;
                if (current == null) {
                    current = MongoClients.create(uri);
                    client = current;
                }
            }
        }
        return current;
    }

    private BsonDocument resumeToken() {
        if (lastResumeToken != null) {
            return lastResumeToken;
        }
        Document stored = state().find(Filters.eq("_id", TOKEN_ID)).first();
        return stored == null ? null : stored.get("token", BsonDocument.class);
    }

    private void saveResumeToken() {
        if (lastResumeToken == null) {
            return;
        }
        state().replaceOne(
                Filters.eq("_id", TOKEN_ID),
                new Document("_id", TOKEN_ID).append("token", lastResumeToken),
                new ReplaceOptions().upsert(true));
    }

    private static Document toDocument(LegacyCustomer customer) {
        return new Document("legalName", customer.legalName())
                .append("partyType", customer.partyType())
                .append("market", customer.market())
                .append("lineOfBusiness", customer.lineOfBusiness())
                .append("contactPointType", customer.contactPointType())
                .append("contactPointValue", customer.contactPointValue())
                .append("productType", customer.productType())
                .append("accountNumber", customer.accountNumber());
    }

    private static LegacyCustomer fromDocument(Document document) {
        return new LegacyCustomer(
                document.getString("legalName"),
                document.getString("partyType"),
                document.getString("market"),
                document.getString("lineOfBusiness"),
                document.getString("contactPointType"),
                document.getString("contactPointValue"),
                document.getString("productType"),
                document.getString("accountNumber"));
    }
}
