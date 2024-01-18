package io.quarkus.mongodb.panache;

import com.mongodb.session.ClientSession;

import io.quarkus.mongodb.panache.runtime.JavaMongoOperations;

public class Panache {

    /**
     * Access the current MongoDB ClientSession from the transaction context.
     * Can be used inside a method annotated with `@Transactional` to manually access the client session.
     *
     * @return ClientSession or null if not in the context of a transaction.
     */
    public static ClientSession getSession() {
        return JavaMongoOperations.INSTANCE.getSession();
    }

    /**
     * Access the current MongoDB ClientSession from the transaction context.
     *
     * @see #getSession()
     *
     * @param entityClass the class of the MongoDB entity in case it is configured to use the non-default client.
     * @return ClientSession or null if not in the context of a transaction.
     */
    public static ClientSession getSession(Class<?> entityClass) {
        return JavaMongoOperations.INSTANCE.getSession(entityClass);
    }
}
