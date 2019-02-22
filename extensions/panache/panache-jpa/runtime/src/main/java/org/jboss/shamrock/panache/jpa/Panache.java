package org.jboss.shamrock.panache.jpa;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;

import org.jboss.shamrock.panache.common.Parameters;
import org.jboss.shamrock.panache.jpa.impl.JpaOperations;

public class Panache {
    
    public static EntityManager getEntityManager() {
        return JpaOperations.getEntityManager();
    }
    
    public static TransactionManager getTransactionManager() {
        return JpaOperations.getTransactionManager();
    }
    
    public static int executeUpdate(String query, Object... params) {
        return JpaOperations.executeUpdate(query, params);
    }
    
    public static int executeUpdate(String query, Map<String,Object> params) {
        return JpaOperations.executeUpdate(query, params);
    }

    public static int executeUpdate(String query, Parameters params) {
        return JpaOperations.executeUpdate(query, params.map());
    }

    public static void setRollbackOnly() {
        JpaOperations.setRollbackOnly();
    }
    
}
