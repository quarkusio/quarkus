package io.quarkus.mongodb.panache.runtime;

import java.util.Map;

import org.bson.BsonDocument;
import org.bson.conversions.Bson;

import com.mongodb.client.MongoCollection;

import io.quarkus.mongodb.panache.PanacheUpdate;
import io.quarkus.panache.common.Parameters;

public class PanacheUpdateImpl implements PanacheUpdate {
    private MongoOperations operations;
    private Class<?> entityClass;
    private Bson update;
    private MongoCollection collection;

    public PanacheUpdateImpl(MongoOperations operations, Class<?> entityClass, Bson update, MongoCollection collection) {
        this.operations = operations;
        this.entityClass = entityClass;
        this.update = update;
        this.collection = collection;
    }

    @Override
    public long where(String query, Object... params) {
        String bindQuery = operations.bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        return collection.updateMany(docQuery, update).getModifiedCount();
    }

    @Override
    public long where(String query, Map<String, Object> params) {
        String bindQuery = operations.bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        return collection.updateMany(docQuery, update).getModifiedCount();
    }

    @Override
    public long where(String query, Parameters params) {
        return where(query, params.map());
    }

    @Override
    public long all() {
        return collection.updateMany(new BsonDocument(), update).getModifiedCount();
    }
}
