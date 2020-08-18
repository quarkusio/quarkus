package io.quarkus.mongodb.panache.reactive.runtime;

import java.util.Map;

import org.bson.BsonDocument;
import org.bson.conversions.Bson;

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
        return collection.updateMany(docQuery, update).map(result -> result.getModifiedCount());
    }

    @Override
    public Uni<Long> where(String query, Map<String, Object> params) {
        String bindQuery = ReactiveMongoOperations.bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        return collection.updateMany(docQuery, update).map(result -> result.getModifiedCount());
    }

    @Override
    public Uni<Long> where(String query, Parameters params) {
        return where(query, params.map());
    }

    @Override
    public Uni<Long> all() {
        return collection.updateMany(new BsonDocument(), update).map(result -> result.getModifiedCount());
    }
}
