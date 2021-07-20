package io.quarkus.mongodb.panache.common.runtime;

import java.util.Map;

import org.bson.BsonDocument;
import org.bson.conversions.Bson;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;

import io.quarkus.mongodb.panache.common.PanacheUpdate;
import io.quarkus.panache.common.Parameters;

public class PanacheUpdateImpl implements PanacheUpdate {
    private MongoOperations operations;
    private Class<?> entityClass;
    private Bson update;
    private MongoCollection collection;
    private ClientSession session;

    public PanacheUpdateImpl(MongoOperations operations, Class<?> entityClass, Bson update, MongoCollection collection) {
        this.operations = operations;
        this.entityClass = entityClass;
        this.update = update;
        this.collection = collection;
        this.session = operations.getSession(entityClass);
    }

    @Override
    public long where(String query, Object... params) {
        String bindQuery = operations.bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        return executeUpdate(docQuery);
    }

    @Override
    public long where(String query, Map<String, Object> params) {
        String bindQuery = operations.bindFilter(entityClass, query, params);
        BsonDocument docQuery = BsonDocument.parse(bindQuery);
        return executeUpdate(docQuery);
    }

    @Override
    public long where(String query, Parameters params) {
        return where(query, params.map());
    }

    @Override
    public long all() {
        BsonDocument all = new BsonDocument();
        return executeUpdate(all);
    }

    private long executeUpdate(BsonDocument query) {
        return session == null ? collection.updateMany(query, update).getModifiedCount()
                : collection.updateMany(session, query, update).getModifiedCount();
    }
}
