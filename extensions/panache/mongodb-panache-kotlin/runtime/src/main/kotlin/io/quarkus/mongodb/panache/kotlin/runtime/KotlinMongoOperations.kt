package io.quarkus.mongodb.panache.kotlin.runtime

import com.mongodb.client.MongoCollection
import io.quarkus.mongodb.panache.PanacheUpdate
import io.quarkus.mongodb.panache.kotlin.PanacheQuery
import io.quarkus.mongodb.panache.runtime.MongoOperations
import io.quarkus.mongodb.panache.runtime.PanacheUpdateImpl
import org.bson.Document
import java.util.stream.Stream

/**
 * Defines kotlin specific implementations of methods needed by [MongoOperations].
 */
class KotlinMongoOperations : MongoOperations<PanacheQuery<*>, PanacheUpdate>() {

    /**
     * Creates the query implementation
     * 
     * @param collection the collection to query
     * @param query the query to base the new query off of
     * @param sortDoc the sort document to use
     * 
     * @return the new query implementation
     */
    override fun createQuery(collection: MongoCollection<*>, query: Document?, sortDoc: Document?) =
            PanacheQueryImpl(collection, query, sortDoc)

    /**
     * Creates the update implementation
     *
     * @param collection the collection to query
     * @param entityClass the type to update
     * @param docUpdate the update document to start with
     *
     * @return the new query implementation
     */
    override fun createUpdate(collection: MongoCollection<*>, entityClass: Class<*>, docUpdate: Document) =
            PanacheUpdateImpl(this, entityClass, docUpdate, collection)

    /**
     * Extracts the query results in to a List.
     * 
     * @param query the query to list
     * 
     * @return a [List] of the results
     */
    override fun list(query: PanacheQuery<*>): List<*> = query.list()

    /**
     * Extracts the query results in to a Stream.
     *
     * @param query the query to stream
     *
     * @return a [Stream] of the results
     */
    override fun stream(query: PanacheQuery<*>): Stream<*> = query.stream()
}