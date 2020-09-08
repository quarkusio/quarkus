package io.quarkus.hibernate.orm.panache;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;

import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;
import io.quarkus.panache.common.Parameters;

/**
 * Utility class for Panache.
 *
 * @author Stéphane Épardaud
 */
public class Panache {

    /**
     * Returns the default {@link EntityManager}
     * 
     * @return {@link EntityManager}
     */
    public static EntityManager getEntityManager() {
        return JpaOperations.getEntityManager();
    }

    /**
     * Returns the {@link EntityManager} for the given {@link Class<?> entity}
     *
     * @return {@link EntityManager}
     */
    public static EntityManager getEntityManager(Class<?> clazz) {
        return JpaOperations.getEntityManager(clazz);
    }

    /**
     * Returns the current {@link TransactionManager}
     * 
     * @return the current {@link TransactionManager}
     */
    public static TransactionManager getTransactionManager() {
        return JpaOperations.getTransactionManager();
    }

    /**
     * Executes a database update operation and return the number of rows operated on.
     * 
     * @param query a normal HQL query
     * @param params optional list of indexed parameters
     * @return the number of rows operated on.
     */
    public static int executeUpdate(String query, Object... params) {
        return JpaOperations.executeUpdate(query, params);
    }

    /**
     * Executes a database update operation and return the number of rows operated on.
     * 
     * @param query a normal HQL query
     * @param params {@link Map} of named parameters
     * @return the number of rows operated on.
     */
    public static int executeUpdate(String query, Map<String, Object> params) {
        return JpaOperations.executeUpdate(query, params);
    }

    /**
     * Executes a database update operation and return the number of rows operated on.
     * 
     * @param query a normal HQL query
     * @param params {@link Parameters} of named parameters
     * @return the number of rows operated on.
     */
    public static int executeUpdate(String query, Parameters params) {
        return JpaOperations.executeUpdate(query, params.map());
    }

    /**
     * Marks the current transaction as "rollback-only", which means that it will not be
     * committed: it will be rolled back at the end of this transaction lifecycle.
     */
    public static void setRollbackOnly() {
        JpaOperations.setRollbackOnly();
    }

}
