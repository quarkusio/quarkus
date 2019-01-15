package org.jboss.panache;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.jboss.protean.arc.Arc;

public class EntityBase {

    // Operations
    
    public void save() {
        EntityManager em = getEntityManager();
        if (!em.contains(this)) {
            em.persist(this);
        }
        em.flush();
    }
    
    public void delete() {
        EntityManager em = getEntityManager();
        em.remove(this);
        em.flush();
    }
    
    public boolean isPersistent() {
        return getEntityManager().contains(this);
    }

    private static EntityManager getEntityManager() {
        return Arc.container().instance(EntityManager.class).get();
    }

    // Queries
    
    public static <T extends EntityBase> T findById(Object id) {
        throw new RuntimeException("Should never be called");
    }

    protected static <T extends EntityBase> T findById(Class<T> entityClass, Object id) {
        return getEntityManager().find(entityClass, id);
    }

    public static <T extends EntityBase> List<T> find(String query, Object... params) {
        throw new RuntimeException("Should never be called");
    }

    protected static <T extends EntityBase> List<T> find(Class<T> entityClass, String query, Object... params) {
        return bindParameters(getEntityManager().createQuery(createFindQuery(entityClass, query, params)), params).getResultList();
    }

    public static <T extends EntityBase> List<T> findAll() {
        throw new RuntimeException("Should never be called");
    }

    protected static <T extends EntityBase> List<T> findAll(Class<T> entityClass) {
        return getEntityManager().createQuery("FROM "+getEntityName(entityClass)).getResultList();
    }

    public static long count() {
        throw new RuntimeException("Should never be called");
    }

    protected static long count(Class<?> entityClass) {
        return (long) getEntityManager().createQuery("SELECT COUNT(*) FROM "+getEntityName(entityClass)).getSingleResult();
    }

    public static long count(String query, Object... params) {
        throw new RuntimeException("Should never be called");
    }

    protected static long count(Class<?> entityClass, String query, Object... params) {
        return (long) bindParameters(getEntityManager().createQuery(createCountQuery(entityClass, query, params)), params).getSingleResult();
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
        return entityClass.getName();
    }
    
    public static long deleteAll() {
        // not implemented yet
        throw new RuntimeException("Should never be called");
    }

    protected static long deleteAll(Class<?> entityClass) {
        return (long) getEntityManager().createQuery("DELETE FROM "+getEntityName(entityClass)).executeUpdate();
    }

    public static long delete(String query, Object... params) {
        throw new RuntimeException("Should never be called");
    }

    protected static long delete(Class<?> entityClass, String query, Object... params) {
        return bindParameters(getEntityManager().createQuery(createDeleteQuery(entityClass, query, params)), params).executeUpdate();
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
}
