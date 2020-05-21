package io.quarkus.mongodb.panache.reactive.runtime;

import java.util.Map;

import org.bson.BsonDocument;
import org.bson.conversions.Bson;

import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.ClientSession;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheUpdate;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.panache.common.Parameters;
import io.smallrye.mutiny.Uni;

public class ReactivePanacheUpdateImpl implements ReactivePanacheUpdate {
    private Class<?> entityClass;
    private Bson update;
    private ReactiveMongoCollection<?> collection;

    public ReactivePanacheUpdateImpl(Class<?> entityClass, Bson update, ReactiveMongoCollection<?> collection) {
        this.entityClass = entityClass;
        this.update = update;
        this.collection = collection;
    }

    @Override
    public Uni<Long> where(String query, Object... params) {
        String bindQuery = ReactiveMongoOperations.bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        return executeUpdate(docQuery, update);
    }

    @Override
    public Uni<Long> where(String query, Map<String, Object> params) {
        String bindQuery = ReactiveMongoOperations.bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        return executeUpdate(docQuery, update);
    }

    @Override
    public Uni<Long> where(String query, Parameters params) {
        return where(query, params.map());
    }

    @Override
    public Uni<Long> all() {
        return executeUpdate(new BsonDocument(), update);
    }

    private Uni<Long> executeUpdate(BsonDocument filter, Bson update) {
        ClientSession session = ReactiveMongoOperations.getSession();
        Uni<UpdateResult> result = session == null ? collection.updateMany(filter, update)
                : collection.updateMany(session, filter, update);
        return result.map(updateResult -> updateResult.getModifiedCount());
    }
}
