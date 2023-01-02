package io.quarkus.mongodb.panache.kotlin

import com.mongodb.session.ClientSession
import io.quarkus.mongodb.panache.kotlin.runtime.KotlinMongoOperations

object Panache {
    /**
     * Access the current MongoDB ClientSession from the transaction context. Can be used inside a
     * method annotated with `@Transactional` to manually access the client session.
     *
     * @return ClientSession or null if not in the context of a transaction.
     */
    val session: ClientSession
        get() = KotlinMongoOperations.INSTANCE.session

    /**
     * Access the current MongoDB ClientSession from the transaction context.
     *
     * @param entityClass the class of the MongoDB entity in case it is configured to use the
     *   non-default client.
     * @return ClientSession or null if not in the context of a transaction.
     * @see [session]
     */
    fun getSession(entityClass: Class<*>?): ClientSession {
        return KotlinMongoOperations.INSTANCE.getSession(entityClass)
    }
}
