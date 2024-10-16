package io.quarkus.mongodb.panache.common.reactive.runtime;

import java.util.Map;

import org.bson.BsonDocument;
import org.bson.conversions.Bson;

import io.quarkus.mongodb.panache.common.reactive.Panache;
import io.quarkus.mongodb.panache.common.reactive.ReactivePanacheUpdate;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.quarkus.panache.common.Parameters;
import io.smallrye.mutiny.Uni;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class ReactivePanacheUpdateImpl implements ReactivePanacheUpdate {
    private final ReactiveMongoOperations operations;
    private final Class<?> entityClass;
    private final Bson update;
    private final ReactiveMongoCollection<?> collection;

    public ReactivePanacheUpdateImpl(ReactiveMongoOperations operations, Class<?> entityClass, Bson update,
            ReactiveMongoCollection<?> collection) {
        this.operations = operations;
        this.entityClass = entityClass;
        this.update = update;
        this.collection = collection;
    }

    @Override
    public Uni<Long> where(String query, Object... params) {
        String bindQuery = operations.bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        return executeUpdate(docQuery);
    }

    @Override
    public Uni<Long> where(String query, Map<String, Object> params) {
        String bindQuery = operations.bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        return executeUpdate(docQuery);
    }

    @Override
    public Uni<Long> where(String query, Parameters params) {
        return where(query, params.map());
    }

    @Override
    public Uni<Long> where(Bson query) {
        return executeUpdate(query);
    }

    @Override
    public Uni<Long> all() {
        return executeUpdate(new BsonDocument());
    }

    private Uni<Long> executeUpdate(Bson query) {
        if (Panache.getCurrentSession() != null) {
            return collection.updateMany(Panache.getCurrentSession(), query, update).map(result -> result.getModifiedCount());
        }
        return collection.updateMany(query, update).map(result -> result.getModifiedCount());
    }
}
