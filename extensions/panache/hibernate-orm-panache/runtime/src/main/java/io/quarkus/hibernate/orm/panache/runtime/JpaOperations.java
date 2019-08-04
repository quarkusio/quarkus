package io.quarkus.hibernate.orm.panache.runtime;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;

public class JpaOperations {

    //
    // Instance methods

    public static void persist(Object entity) {
        EntityManager em = getEntityManager();
        persist(em, entity);
    }

    public static void persist(EntityManager em, Object entity) {
        if (!em.contains(entity)) {
            em.persist(entity);
        }
    }

    public static void persist(Iterable<?> entities) {
        EntityManager em = getEntityManager();
        for (Object entity : entities) {
            persist(em, entity);
        }
    }

    public static void persist(Object firstEntity, Object... entities) {
        EntityManager em = getEntityManager();
        persist(firstEntity);
        for (Object entity : entities) {
            persist(em, entity);
        }
    }

    public static void persist(Stream<?> entities) {
        EntityManager em = getEntityManager();
        entities.forEach(entity -> persist(em, entity));
    }

    public static void delete(Object entity) {
        EntityManager em = getEntityManager();
        em.remove(entity);
    }

    public static boolean isPersistent(Object entity) {
        return getEntityManager().contains(entity);
    }

    public static void flush() {
        getEntityManager().flush();
    }

    //
    // Private stuff

    public static EntityManager getEntityManager() {
        EntityManager entityManager = Arc.container().instance(EntityManager.class).get();
        if (entityManager == null) {
            throw new PersistenceException("No EntityManager found. Do you have any JPA entities defined?");
        }
        return entityManager;
    }

    public static TransactionManager getTransactionManager() {
        return Arc.container().instance(TransactionManager.class).get();
    }

    public static Query bindParameters(Query query, Object[] params) {
        if (params == null || params.length == 0)
            return query;
        for (int i = 0; i < params.length; i++) {
            query.setParameter(i + 1, params[i]);
        }
        return query;
    }

    public static Query bindParameters(Query query, Map<String, Object> params) {
        if (params == null || params.size() == 0)
            return query;
        for (Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        return query;
    }

    private static int paramCount(Object[] params) {
        return params != null ? params.length : 0;
    }

    private static int paramCount(Map<String, Object> params) {
        return params != null ? params.size() : 0;
    }

    private static String getEntityName(Class<?> entityClass) {
        // FIXME: not true?
        return entityClass.getName();
    }

    private static String createFindQuery(Class<?> entityClass, String query, int paramCount) {
        if (query == null)
            return "FROM " + getEntityName(entityClass);

        String trimmed = query.trim();
        if (trimmed.isEmpty())
            return "FROM " + getEntityName(entityClass);

        String trimmedLc = trimmed.toLowerCase();
        if (trimmedLc.startsWith("from ") || trimmedLc.startsWith("select ")) {
            return query;
        }
        if (trimmedLc.startsWith("order by ")) {
            return "FROM " + getEntityName(entityClass) + " " + query;
        }
        if (trimmedLc.indexOf(' ') == -1 && trimmedLc.indexOf('=') == -1 && paramCount == 1) {
            query += " = ?1";
        }
        return "FROM " + getEntityName(entityClass) + " WHERE " + query;
    }

    private static String createCountQuery(Class<?> entityClass, String query, int paramCount) {
        if (query == null)
            return "SELECT COUNT(*) FROM " + getEntityName(entityClass);

        String trimmed = query.trim();
        if (trimmed.isEmpty())
            return "SELECT COUNT(*) FROM " + getEntityName(entityClass);

        String trimmedLc = trimmed.toLowerCase();
        if (trimmedLc.startsWith("from ")) {
            return "SELECT COUNT(*) " + query;
        }
        if (trimmedLc.startsWith("order by ")) {
            // ignore it
            return "SELECT COUNT(*) FROM " + getEntityName(entityClass);
        }
        if (trimmedLc.indexOf(' ') == -1 && trimmedLc.indexOf('=') == -1 && paramCount == 1) {
            query += " = ?1";
        }
        return "SELECT COUNT(*) FROM " + getEntityName(entityClass) + " WHERE " + query;
    }

    private static String createDeleteQuery(Class<?> entityClass, String query, int paramCount) {
        if (query == null)
            return "DELETE FROM " + getEntityName(entityClass);

        String trimmed = query.trim();
        if (trimmed.isEmpty())
            return "DELETE FROM " + getEntityName(entityClass);

        String trimmedLc = trimmed.toLowerCase();
        if (trimmedLc.startsWith("from ")) {
            return "DELETE " + query;
        }
        if (trimmedLc.startsWith("order by ")) {
            // ignore it
            return "DELETE FROM " + getEntityName(entityClass);
        }
        if (trimmedLc.indexOf(' ') == -1 && trimmedLc.indexOf('=') == -1 && paramCount == 1) {
            query += " = ?1";
        }
        return "DELETE FROM " + getEntityName(entityClass) + " WHERE " + query;
    }

    private static String toOrderBy(Sort sort) {
        StringBuilder sb = new StringBuilder(" ORDER BY ");
        for (int i = 0; i < sort.getColumns().size(); i++) {
            Sort.Column column = sort.getColumns().get(i);
            if (i > 0)
                sb.append(" , ");
            sb.append(column.getName());
            if (column.getDirection() != Sort.Direction.Ascending)
                sb.append(" DESC");
        }
        return sb.toString();
    }

    //
    // Queries

    public static Object findById(Class<?> entityClass, Object id) {
        return getEntityManager().find(entityClass, id);
    }

    public static PanacheQuery<?> find(Class<?> entityClass, String query, Object... params) {
        return find(entityClass, query, null, params);
    }

    @SuppressWarnings("rawtypes")
    public static PanacheQuery<?> find(Class<?> entityClass, String query, Sort sort, Object... params) {
        String findQuery = createFindQuery(entityClass, query, paramCount(params));
        EntityManager em = getEntityManager();
        // FIXME: check for duplicate ORDER BY clause?
        Query jpaQuery = em.createQuery(sort != null ? findQuery + toOrderBy(sort) : findQuery);
        bindParameters(jpaQuery, params);
        return new PanacheQueryImpl(em, jpaQuery, findQuery, params);
    }

    public static PanacheQuery<?> find(Class<?> entityClass, String query, Map<String, Object> params) {
        return find(entityClass, query, null, params);
    }

    @SuppressWarnings("rawtypes")
    public static PanacheQuery<?> find(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        String findQuery = createFindQuery(entityClass, query, paramCount(params));
        EntityManager em = getEntityManager();
        // FIXME: check for duplicate ORDER BY clause?
        Query jpaQuery = em.createQuery(sort != null ? findQuery + toOrderBy(sort) : findQuery);
        bindParameters(jpaQuery, params);
        return new PanacheQueryImpl(em, jpaQuery, findQuery, params);
    }

    public static PanacheQuery<?> find(Class<?> entityClass, String query, Parameters params) {
        return find(entityClass, query, null, params);
    }

    public static PanacheQuery<?> find(Class<?> entityClass, String query, Sort sort, Parameters params) {
        return find(entityClass, query, sort, params.map());
    }

    public static List<?> list(Class<?> entityClass, String query, Object... params) {
        return find(entityClass, query, params).list();
    }

    public static List<?> list(Class<?> entityClass, String query, Sort sort, Object... params) {
        return find(entityClass, query, sort, params).list();
    }

    public static List<?> list(Class<?> entityClass, String query, Map<String, Object> params) {
        return find(entityClass, query, params).list();
    }

    public static List<?> list(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        return find(entityClass, query, sort, params).list();
    }

    public static List<?> list(Class<?> entityClass, String query, Parameters params) {
        return find(entityClass, query, params).list();
    }

    public static List<?> list(Class<?> entityClass, String query, Sort sort, Parameters params) {
        return find(entityClass, query, sort, params).list();
    }

    public static Stream<?> stream(Class<?> entityClass, String query, Object... params) {
        return find(entityClass, query, params).stream();
    }

    public static Stream<?> stream(Class<?> entityClass, String query, Sort sort, Object... params) {
        return find(entityClass, query, sort, params).stream();
    }

    public static Stream<?> stream(Class<?> entityClass, String query, Map<String, Object> params) {
        return find(entityClass, query, params).stream();
    }

    public static Stream<?> stream(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        return find(entityClass, query, sort, params).stream();
    }

    public static Stream<?> stream(Class<?> entityClass, String query, Parameters params) {
        return find(entityClass, query, params).stream();
    }

    public static Stream<?> stream(Class<?> entityClass, String query, Sort sort, Parameters params) {
        return find(entityClass, query, sort, params).stream();
    }

    @SuppressWarnings("rawtypes")
    public static PanacheQuery<?> findAll(Class<?> entityClass) {
        String query = "FROM " + getEntityName(entityClass);
        EntityManager em = getEntityManager();
        return new PanacheQueryImpl(em, em.createQuery(query), query, null);
    }

    @SuppressWarnings("rawtypes")
    public static PanacheQuery<?> findAll(Class<?> entityClass, Sort sort) {
        String query = "FROM " + getEntityName(entityClass);
        String sortedQuery = query + toOrderBy(sort);
        EntityManager em = getEntityManager();
        return new PanacheQueryImpl(em, em.createQuery(sortedQuery), query, null);
    }

    public static List<?> listAll(Class<?> entityClass) {
        return findAll(entityClass).list();
    }

    public static List<?> listAll(Class<?> entityClass, Sort sort) {
        return findAll(entityClass, sort).list();
    }

    public static Stream<?> streamAll(Class<?> entityClass) {
        return findAll(entityClass).stream();
    }

    public static Stream<?> streamAll(Class<?> entityClass, Sort sort) {
        return findAll(entityClass, sort).stream();
    }

    public static long count(Class<?> entityClass) {
        return (long) getEntityManager().createQuery("SELECT COUNT(*) FROM " + getEntityName(entityClass)).getSingleResult();
    }

    public static long count(Class<?> entityClass, String query, Object... params) {
        return (long) bindParameters(getEntityManager().createQuery(createCountQuery(entityClass, query, paramCount(params))),
                params).getSingleResult();
    }

    public static long count(Class<?> entityClass, String query, Map<String, Object> params) {
        return (long) bindParameters(getEntityManager().createQuery(createCountQuery(entityClass, query, paramCount(params))),
                params).getSingleResult();
    }

    public static long count(Class<?> entityClass, String query, Parameters params) {
        return count(entityClass, query, params.map());
    }

    public static long deleteAll(Class<?> entityClass) {
        return (long) getEntityManager().createQuery("DELETE FROM " + getEntityName(entityClass)).executeUpdate();
    }

    public static long delete(Class<?> entityClass, String query, Object... params) {
        return bindParameters(getEntityManager().createQuery(createDeleteQuery(entityClass, query, paramCount(params))), params)
                .executeUpdate();
    }

    public static long delete(Class<?> entityClass, String query, Map<String, Object> params) {
        return bindParameters(getEntityManager().createQuery(createDeleteQuery(entityClass, query, paramCount(params))), params)
                .executeUpdate();
    }

    public static long delete(Class<?> entityClass, String query, Parameters params) {
        return delete(entityClass, query, params.map());
    }

    public static IllegalStateException implementationInjectionMissing() {
        return new IllegalStateException(
                "This method is normally automatically overridden in subclasses: did you forget to annotate your entity with @Entity?");
    }

    public static int executeUpdate(String query, Object... params) {
        Query jpaQuery = getEntityManager().createQuery(query);
        bindParameters(jpaQuery, params);
        return jpaQuery.executeUpdate();
    }

    public static int executeUpdate(String query, Map<String, Object> params) {
        Query jpaQuery = getEntityManager().createQuery(query);
        bindParameters(jpaQuery, params);
        return jpaQuery.executeUpdate();
    }

    public static void setRollbackOnly() {
        try {
            getTransactionManager().setRollbackOnly();
        } catch (SystemException e) {
            throw new IllegalStateException(e);
        }
    }

}
