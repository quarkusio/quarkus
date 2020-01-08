package io.quarkus.mongodb;

/**
 * A reactive API to interact with a Mongo collection.
 *
 * @param <T> The type that this collection will encode documents from and decode documents to.
 * @since 1.0
 */
@Deprecated
public interface ReactiveMongoCollection<T> extends io.quarkus.mongodb.axle.ReactiveMongoCollection<T> {

}
