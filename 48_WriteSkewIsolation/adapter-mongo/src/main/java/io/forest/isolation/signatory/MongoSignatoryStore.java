package io.forest.isolation.signatory;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.util.function.Function;

/**
 * {@link SignatoryStore} over the raw MongoDB sync driver. MongoDB is snapshot isolated, so a
 * transaction is used as-is (snapshot read concern, majority write concern); the isolation level is
 * never lowered or raised here.
 */
public final class MongoSignatoryStore implements SignatoryStore {

    private final MongoClient client;
    private final String database;
    private final String signatoryCollection;
    private final String counterCollection;

    public MongoSignatoryStore(MongoClient client,
                               String database,
                               String signatoryCollection,
                               String counterCollection) {
        this.client = client;
        this.database = database;
        this.signatoryCollection = signatoryCollection;
        this.counterCollection = counterCollection;
    }

    @Override
    public <T> T inTransaction(Function<Tx, T> action) {
        try (ClientSession session = client.startSession()) {
            session.startTransaction();
            try {
                T result = action.apply(new MongoTx(session, signatory(), counter()));
                session.commitTransaction();
                return result;
            } catch (RuntimeException e) {
                session.abortTransaction();
                throw e;
            }
        }
    }

    private MongoCollection<Document> signatory() {
        return client.getDatabase(database).getCollection(signatoryCollection);
    }

    private MongoCollection<Document> counter() {
        return client.getDatabase(database).getCollection(counterCollection);
    }

    private static final class MongoTx implements Tx {

        private final ClientSession session;
        private final MongoCollection<Document> signatory;
        private final MongoCollection<Document> counter;

        private MongoTx(ClientSession session,
                        MongoCollection<Document> signatory,
                        MongoCollection<Document> counter) {
            this.session = session;
            this.signatory = signatory;
            this.counter = counter;
        }

        @Override
        public int countAuthorized(String partyId) {
            return (int) signatory.countDocuments(session,
                    Filters.and(Filters.eq("party_id", partyId), Filters.eq("authorized", true)));
        }

        @Override
        public void setAuthorized(String partyId, String signatoryId, boolean authorized) {
            signatory.updateOne(session, Filters.eq("_id", signatoryId),
                    Updates.set("authorized", authorized));
        }

        @Override
        public void touch(String partyId) {
            counter.updateOne(session, Filters.eq("_id", partyId), Updates.inc("seq", 1));
        }
    }
}
