package io.forest.isolation.signatory;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Boots a real single-node MongoDB replica set (transactions require a replica set) and exposes a
 * snapshot-isolated {@link SignatoryStore}. The replica set is named {@code rs0} and advertises
 * {@code localhost:27017}, so a client on the host reaches it directly.
 */
final class MongoFixture {

    private static final String URI = "mongodb://localhost:27017/?replicaSet=rs0&serverSelectionTimeoutMS=2000";
    private static final String COMPOSE_FILE = Compose.file("docker-compose.mongo.yml");
    private static final String COMPOSE_PROJECT = "write-skew-mongo";

    private static final String DATABASE = "cdm";
    private static final String SIGNATORY = "signatory";
    private static final String COUNTER = "party_counter";
    private static final String PARTY_ID = "P1";

    private static MongoClient client;
    private static SignatoryStore store;

    private MongoFixture() {
    }

    /** Boots the engine on first use, then reuses it for the rest of the suite. */
    static synchronized SignatoryStore store() {
        if (store == null) {
            Compose.up(COMPOSE_PROJECT, COMPOSE_FILE);
            Runtime.getRuntime().addShutdownHook(new Thread(MongoFixture::shutdown));
            client = MongoClients.create(URI);
            awaitPrimary();
            store = new MongoSignatoryStore(client, DATABASE, SIGNATORY, COUNTER);
            reset();
        }
        return store;
    }

    /** Resets party P1 to exactly two authorized signatories and a zeroed counter. */
    static void reset() {
        var database = client.getDatabase(DATABASE);
        database.getCollection(SIGNATORY).deleteMany(new Document());
        database.getCollection(SIGNATORY).insertMany(List.of(
                new Document("_id", "alice").append("party_id", PARTY_ID).append("authorized", true),
                new Document("_id", "bob").append("party_id", PARTY_ID).append("authorized", true)));
        database.getCollection(COUNTER).deleteMany(new Document());
        database.getCollection(COUNTER).insertOne(new Document("_id", PARTY_ID).append("seq", 0));
    }

    /**
     * The committed rows that back the invariant, rendered for the evidence report. Read outside any
     * transaction, so it reflects exactly what both concurrent transactions left behind.
     */
    static String snapshot() {
        if (client == null) {
            return "<engine not started>";
        }
        var database = client.getDatabase(DATABASE);
        StringBuilder rows = new StringBuilder();
        for (Document document : database.getCollection(SIGNATORY).find().sort(new Document("_id", 1))) {
            rows.append(document.getString("_id")).append('=').append(document.getBoolean("authorized"))
                    .append(' ');
        }
        Document counter = database.getCollection(COUNTER).find(new Document("_id", PARTY_ID)).first();
        return "signatory[" + rows.toString().strip() + "] party_counter.seq="
                + (counter == null ? "<none>" : counter.get("seq"));
    }

    private static void awaitPrimary() {
        Instant deadline = Instant.now().plus(Duration.ofMinutes(3));
        while (Instant.now().isBefore(deadline)) {
            try {
                Document hello = client.getDatabase("admin").runCommand(new Document("hello", 1));
                if (Boolean.TRUE.equals(hello.getBoolean("isWritablePrimary"))) {
                    return;
                }
            } catch (RuntimeException stillForming) {
                // the replica set has not elected a primary yet
            }
            sleep();
        }
        throw new IllegalStateException("The MongoDB replica set did not elect a primary in time");
    }

    private static void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void shutdown() {
        if (client != null) {
            client.close();
        }
        Compose.down(COMPOSE_PROJECT, COMPOSE_FILE);
    }
}
