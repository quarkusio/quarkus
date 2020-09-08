package io.quarkus.mongodb.panache.runtime;

import java.util.List;
import java.util.stream.Stream;

import org.bson.Document;

import com.mongodb.client.MongoCollection;

import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.mongodb.panache.PanacheUpdate;

public class JavaMongoOperations extends MongoOperations<PanacheQuery<?>, PanacheUpdate> {
    @Override
    protected PanacheQuery<?> createQuery(MongoCollection collection, Document query, Document sortDoc) {
        return new PanacheQueryImpl(collection, query, sortDoc);
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
