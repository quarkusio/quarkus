package io.quarkus.mongodb.panache.reactive.runtime;

import java.util.List;

import org.bson.Document;

import io.quarkus.mongodb.panache.common.reactive.runtime.ReactiveMongoOperations;
import io.quarkus.mongodb.panache.common.reactive.runtime.ReactivePanacheUpdateImpl;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheUpdate;
import io.quarkus.mongodb.reactive.ReactiveMongoCollection;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class JavaReactiveMongoOperations extends ReactiveMongoOperations<ReactivePanacheQuery<?>, ReactivePanacheUpdate> {
    public static final JavaReactiveMongoOperations INSTANCE = new JavaReactiveMongoOperations();

    @Override
    protected ReactivePanacheQuery<?> createQuery(ReactiveMongoCollection collection, Document query, Document sortDoc) {
        return new ReactivePanacheQueryImpl(collection, query, sortDoc);
    }

    @Override
    protected ReactivePanacheUpdate createUpdate(ReactiveMongoCollection<?> collection, Class<?> entityClass,
            Document docUpdate) {
        return new ReactivePanacheUpdateImpl(this, entityClass, docUpdate, collection);
    }

    @Override
    protected Uni<? extends List<?>> list(ReactivePanacheQuery<?> query) {
        return query.list();
    }

    @Override
    protected Multi<?> stream(ReactivePanacheQuery<?> query) {
        return query.stream();
    }
}
