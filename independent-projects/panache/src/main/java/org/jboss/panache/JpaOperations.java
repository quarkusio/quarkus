package org.jboss.panache;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.jboss.protean.arc.Arc;

public class JpaOperations {

    //
    // Instance methods
    
    static void save(Object entity) {
        EntityManager em = getEntityManager();
        if (!em.contains(entity)) {
            em.persist(entity);
        }
        em.flush();
    }

    static void delete(Object entity) {
        EntityManager em = getEntityManager();
        em.remove(entity);
        em.flush();
    }

    static boolean isPersistent(Object entity) {
        return getEntityManager().contains(entity);
    }

    //
    // Private stuff
    
    private static EntityManager getEntityManager() {
        return Arc.container().instance(EntityManager.class).get();
    }

    private static Query bindParameters(Query query, Object[] params) {
        if(params == null || params.length == 0)
            return query;
        // FIXME: support Map<String,Object> as single param value?
        for (int i = 0; i < params.length; i++) {
            query.setParameter(i+1,  params[i]);
        }
        return query;
    }

    private static String getEntityName(Class<?> entityClass) {
        // FIXME: not true?
        return entityClass.getName();
    }

    private static String createFindQuery(Class<?> entityClass, String query, Object[] params) {
        if(query == null)
            return "FROM "+getEntityName(entityClass);

        String trimmed = query.trim();
        if(trimmed.isEmpty())
            return "FROM "+getEntityName(entityClass);
        
        String lc = query.toLowerCase();
        if(lc.startsWith("from ") || lc.startsWith("select ")) {
            return query;
        }
        if(lc.startsWith("order by ")) {
            return "FROM "+getEntityName(entityClass) + " " + query;
        }
        return "FROM "+getEntityName(entityClass)+" WHERE "+query;
    }

    private static String createCountQuery(Class<?> entityClass, String query, Object[] params) {
        if(query == null)
            return "SELECT COUNT(*) FROM "+getEntityName(entityClass);

        String trimmed = query.trim();
        if(trimmed.isEmpty())
            return "SELECT COUNT(*) FROM "+getEntityName(entityClass);
        
        String lc = query.toLowerCase();
        if(lc.startsWith("from ")) {
            return "SELECT COUNT(*) "+query;
        }
        if(lc.startsWith("order by ")) {
            // ignore it
            return "SELECT COUNT(*) FROM "+getEntityName(entityClass);
        }
        return "SELECT COUNT(*) FROM "+getEntityName(entityClass)+" WHERE "+query;
    }

    private static String createDeleteQuery(Class<?> entityClass, String query, Object[] params) {
        if(query == null)
            return "DELETE FROM "+getEntityName(entityClass);

        String trimmed = query.trim();
        if(trimmed.isEmpty())
            return "DELETE FROM "+getEntityName(entityClass);
        
        String lc = query.toLowerCase();
        if(lc.startsWith("from ")) {
            return "DELETE "+query;
        }
        if(lc.startsWith("order by ")) {
            // ignore it
            return "DELETE FROM "+getEntityName(entityClass);
        }
        return "DELETE FROM "+getEntityName(entityClass)+" WHERE "+query;
    }

    //
    // Queries
    
    public static Object findById(Class<?> entityClass, Object id) {
        return getEntityManager().find(entityClass, id);
    }

    public static List<?> find(Class<?> entityClass, String query, Object... params) {
        return bindParameters(getEntityManager().createQuery(createFindQuery(entityClass, query, params)), params).getResultList();
    }

    public static List<?> findAll(Class<?> entityClass) {
        return getEntityManager().createQuery("FROM "+getEntityName(entityClass)).getResultList();
    }

    public static long count(Class<?> entityClass) {
        return (long) getEntityManager().createQuery("SELECT COUNT(*) FROM "+getEntityName(entityClass)).getSingleResult();
    }

    public static long count(Class<?> entityClass, String query, Object... params) {
        return (long) bindParameters(getEntityManager().createQuery(createCountQuery(entityClass, query, params)), params).getSingleResult();
    }

    public static long deleteAll(Class<?> entityClass) {
        return (long) getEntityManager().createQuery("DELETE FROM "+getEntityName(entityClass)).executeUpdate();
    }

    public static long delete(Class<?> entityClass, String query, Object... params) {
        return bindParameters(getEntityManager().createQuery(createDeleteQuery(entityClass, query, params)), params).executeUpdate();
    }
}
