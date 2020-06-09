package io.quarkus.mongodb.panache.kotlin.reactive.runtime

import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheQuery
import io.quarkus.mongodb.panache.reactive.ReactivePanacheUpdate
import io.quarkus.mongodb.panache.reactive.runtime.ReactiveMongoOperations
import io.quarkus.mongodb.panache.reactive.runtime.ReactivePanacheUpdateImpl
import io.quarkus.mongodb.panache.runtime.MongoOperations
import io.quarkus.mongodb.reactive.ReactiveMongoCollection
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import org.bson.Document
import java.util.stream.Stream

/**
 * Defines kotlin specific implementations of methods needed by [ReactiveMongoOperations].
 */
class KotlinReactiveMongoOperations : ReactiveMongoOperations<ReactivePanacheQuery<*>, ReactivePanacheUpdate>() {
    /**
     * Creates the query implementation
     *
     * @param collection the collection to query
     * @param query the query to base the new query off of
     * @param sortDoc the sort document to use
     *
     * @return the new query implementation
     */
    override fun createQuery(collection: ReactiveMongoCollection<*>, query: Document?, sortDoc: Document?) =
            ReactivePanacheQueryImpl(collection, query, sortDoc)

    /**
     * Creates the update implementation
     *
     * @param collection the collection to query
     * @param entityClass the type to update
     * @param docUpdate the update document to start with
     *
     * @return the new query implementation
     */
    override fun createUpdate(collection: ReactiveMongoCollection<*>, entityClass: Class<*>, docUpdate: Document) =
            ReactivePanacheUpdateImpl(this, entityClass, docUpdate, collection)

    /**
     * Extracts the query results in to a List.
     *
     * @param query the query to list
     *
     * @return the query results
     */
    override fun list(query: ReactivePanacheQuery<*>): Uni<List<*>> = query.list() as Uni<List<*>>

    /**
     * Extracts the query results in to a Stream.
     *
     * @param query the query to stream
     *
     * @return the query results
     */
    override fun stream(query: ReactivePanacheQuery<*>): Multi<*> = query.stream()
}