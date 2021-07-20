package io.quarkus.mongodb.panache.runtime;

import java.util.List;
import java.util.stream.Stream;

import org.bson.Document;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;

import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.mongodb.panache.PanacheUpdate;
import io.quarkus.mongodb.panache.common.runtime.MongoOperations;
import io.quarkus.mongodb.panache.common.runtime.PanacheUpdateImpl;

public class JavaMongoOperations extends MongoOperations<PanacheQuery<?>, PanacheUpdate> {
    /**
     * Provides the default implementations for quarkus to wire up. Should not be used by third party developers.
     */
    public static final JavaMongoOperations INSTANCE = new JavaMongoOperations();

    @Override
    protected PanacheQuery<?> createQuery(MongoCollection collection, ClientSession session, Document query, Document sortDoc) {
        return new PanacheQueryImpl(collection, session, query, sortDoc);
    }

    @Override
    protected PanacheUpdate createUpdate(MongoCollection collection, Class<?> entityClass, Document docUpdate) {
        return new PanacheUpdateImpl(this, entityClass, docUpdate, collection);
    }

    @Override
    protected List<?> list(PanacheQuery<?> query) {
        return query.list();
    }

    @Override
    protected Stream<?> stream(PanacheQuery<?> query) {
        return query.stream();
    }
}
