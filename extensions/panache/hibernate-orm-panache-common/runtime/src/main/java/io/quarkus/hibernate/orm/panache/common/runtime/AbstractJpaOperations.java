package io.quarkus.hibernate.orm.panache.common.runtime;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.common.exception.PanacheQueryException;

public abstract class AbstractJpaOperations<PanacheQueryType> {

    protected abstract PanacheQueryType createPanacheQuery(EntityManager em, String query, String orderBy,
            Object paramsArrayOrMap);

    protected abstract List<?> list(PanacheQueryType query);

    protected abstract Stream<?> stream(PanacheQueryType query);

    public abstract EntityManager getEntityManager(Class<?> clazz);

    public EntityManager getEntityManager(String persistentUnitName) {
        if (persistentUnitName == null || PersistenceUnitUtil.isDefaultPersistenceUnit(persistentUnitName)) {
            return Arc.container().instance(EntityManager.class).get();
        }

        PersistenceUnit.PersistenceUnitLiteral persistenceUnitLiteral = new PersistenceUnit.PersistenceUnitLiteral(
                persistentUnitName);
        return Arc.container().instance(EntityManager.class, persistenceUnitLiteral).get();
    }

    public EntityManager getEntityManager() {
        return getEntityManager(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME);
    }
    //
    // Instance methods

    public void persist(Object entity) {
        EntityManager em = getEntityManager(entity.getClass());
        persist(em, entity);
    }

    public void persist(EntityManager em, Object entity) {
        if (!em.contains(entity)) {
            em.persist(entity);
        }
    }

    public void persist(Iterable<?> entities) {
        for (Object entity : entities) {
            persist(getEntityManager(entity.getClass()), entity);
        }
    }

    public void persist(Object firstEntity, Object... entities) {
        persist(firstEntity);
        for (Object entity : entities) {
            persist(entity);
        }
    }

    public void persist(Stream<?> entities) {
        entities.forEach(entity -> persist(entity));
    }

    public void delete(Object entity) {
        EntityManager em = getEntityManager(entity.getClass());
        em.remove(entity);
    }

    public boolean isPersistent(Object entity) {
        return getEntityManager(entity.getClass()).contains(entity);
    }

    public void flush(Object entityClass) {
        getEntityManager(entityClass.getClass()).flush();
    }
    //
    // Private stuff

    public TransactionManager getTransactionManager() {
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

    public int paramCount(Object[] params) {
        return params != null ? params.length : 0;
    }

    public int paramCount(Map<String, Object> params) {
        return params != null ? params.size() : 0;
    }

    private String getEntityName(Class<?> entityClass) {
        // FIXME: not true?
        return entityClass.getName();
    }

    public String createFindQuery(Class<?> entityClass, String query, int paramCount) {
        if (query == null) {
            return "FROM " + getEntityName(entityClass);
        }

        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            return "FROM " + getEntityName(entityClass);
        }

        if (isNamedQuery(query)) {
            // we return named query as is
            return query;
        }

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

    static boolean isNamedQuery(String query) {
        if (query == null || query.isEmpty()) {
            return false;
        }
        return query.charAt(0) == '#';
    }

    private String createCountQuery(Class<?> entityClass, String query, int paramCount) {
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

    private String createUpdateQuery(Class<?> entityClass, String query, int paramCount) {
        if (query == null) {
            throw new PanacheQueryException("Query string cannot be null");
        }

        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            throw new PanacheQueryException("Query string cannot be empty");
        }

        String trimmedLc = trimmed.toLowerCase();
        if (trimmedLc.startsWith("update ")) {
            return query;
        }
        if (trimmedLc.startsWith("from ")) {
            return "UPDATE " + query;
        }
        if (trimmedLc.indexOf(' ') == -1 && trimmedLc.indexOf('=') == -1 && paramCount == 1) {
            query += " = ?1";
        }
        if (trimmedLc.startsWith("set ")) {
            return "UPDATE FROM " + getEntityName(entityClass) + " " + query;
        }
        return "UPDATE FROM " + getEntityName(entityClass) + " SET " + query;
    }

    private String createDeleteQuery(Class<?> entityClass, String query, int paramCount) {
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

    public String toOrderBy(Sort sort) {
        if (sort == null) {
            return null;
        }
        if (sort.getColumns().size() == 0) {
            return "";
        }
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

    public Object findById(Class<?> entityClass, Object id) {
        return getEntityManager(entityClass).find(entityClass, id);
    }

    public Object findById(Class<?> entityClass, Object id, LockModeType lockModeType) {
        return getEntityManager(entityClass).find(entityClass, id, lockModeType);
    }

    public Optional<?> findByIdOptional(Class<?> entityClass, Object id) {
        return Optional.ofNullable(findById(entityClass, id));
    }

    public Optional<?> findByIdOptional(Class<?> entityClass, Object id, LockModeType lockModeType) {
        return Optional.ofNullable(findById(entityClass, id, lockModeType));
    }

    public PanacheQueryType find(Class<?> entityClass, String query, Object... params) {
        return find(entityClass, query, null, params);
    }

    public PanacheQueryType find(Class<?> entityClass, String query, Sort sort, Object... params) {
        String findQuery = createFindQuery(entityClass, query, paramCount(params));
        EntityManager em = getEntityManager(entityClass);
        // FIXME: check for duplicate ORDER BY clause?
        if (isNamedQuery(query)) {
            String namedQuery = query.substring(1);
            NamedQueryUtil.checkNamedQuery(entityClass, namedQuery);
            return createPanacheQuery(em, query, toOrderBy(sort), params);
        }
        return createPanacheQuery(em, findQuery, toOrderBy(sort), params);
    }

    public PanacheQueryType find(Class<?> entityClass, String query, Map<String, Object> params) {
        return find(entityClass, query, null, params);
    }

    public PanacheQueryType find(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        String findQuery = createFindQuery(entityClass, query, paramCount(params));
        EntityManager em = getEntityManager(entityClass);
        // FIXME: check for duplicate ORDER BY clause?
        if (isNamedQuery(query)) {
            String namedQuery = query.substring(1);
            NamedQueryUtil.checkNamedQuery(entityClass, namedQuery);
            return createPanacheQuery(em, query, toOrderBy(sort), params);
        }
        return createPanacheQuery(em, findQuery, toOrderBy(sort), params);
    }

    public PanacheQueryType find(Class<?> entityClass, String query, Parameters params) {
        return find(entityClass, query, null, params);
    }

    public PanacheQueryType find(Class<?> entityClass, String query, Sort sort, Parameters params) {
        return find(entityClass, query, sort, params.map());
    }

    public List<?> list(Class<?> entityClass, String query, Object... params) {
        return list(find(entityClass, query, params));
    }

    public List<?> list(Class<?> entityClass, String query, Sort sort, Object... params) {
        return list(find(entityClass, query, sort, params));
    }

    public List<?> list(Class<?> entityClass, String query, Map<String, Object> params) {
        return list(find(entityClass, query, params));
    }

    public List<?> list(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        return list(find(entityClass, query, sort, params));
    }

    public List<?> list(Class<?> entityClass, String query, Parameters params) {
        return list(find(entityClass, query, params));
    }

    public List<?> list(Class<?> entityClass, String query, Sort sort, Parameters params) {
        return list(find(entityClass, query, sort, params));
    }

    public Stream<?> stream(Class<?> entityClass, String query, Object... params) {
        return stream(find(entityClass, query, params));
    }

    public Stream<?> stream(Class<?> entityClass, String query, Sort sort, Object... params) {
        return stream(find(entityClass, query, sort, params));
    }

    public Stream<?> stream(Class<?> entityClass, String query, Map<String, Object> params) {
        return stream(find(entityClass, query, params));
    }

    public Stream<?> stream(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        return stream(find(entityClass, query, sort, params));
    }

    public Stream<?> stream(Class<?> entityClass, String query, Parameters params) {
        return stream(find(entityClass, query, params));
    }

    public Stream<?> stream(Class<?> entityClass, String query, Sort sort, Parameters params) {
        return stream(find(entityClass, query, sort, params));
    }

    public PanacheQueryType findAll(Class<?> entityClass) {
        String query = "FROM " + getEntityName(entityClass);
        EntityManager em = getEntityManager(entityClass);
        return createPanacheQuery(em, query, null, null);
    }

    public PanacheQueryType findAll(Class<?> entityClass, Sort sort) {
        String query = "FROM " + getEntityName(entityClass);
        EntityManager em = getEntityManager(entityClass);
        return createPanacheQuery(em, query, toOrderBy(sort), null);
    }

    public List<?> listAll(Class<?> entityClass) {
        return list(findAll(entityClass));
    }

    public List<?> listAll(Class<?> entityClass, Sort sort) {
        return list(findAll(entityClass, sort));
    }

    public Stream<?> streamAll(Class<?> entityClass) {
        return stream(findAll(entityClass));
    }

    public Stream<?> streamAll(Class<?> entityClass, Sort sort) {
        return stream(findAll(entityClass, sort));
    }

    public long count(Class<?> entityClass) {
        return (long) getEntityManager(entityClass).createQuery("SELECT COUNT(*) FROM " + getEntityName(entityClass))
                .getSingleResult();
    }

    public long count(Class<?> entityClass, String query, Object... params) {
        return (long) bindParameters(
                getEntityManager(entityClass).createQuery(createCountQuery(entityClass, query, paramCount(params))),
                params).getSingleResult();
    }

    public long count(Class<?> entityClass, String query, Map<String, Object> params) {
        return (long) bindParameters(
                getEntityManager(entityClass).createQuery(createCountQuery(entityClass, query, paramCount(params))),
                params).getSingleResult();
    }

    public long count(Class<?> entityClass, String query, Parameters params) {
        return count(entityClass, query, params.map());
    }

    public boolean exists(Class<?> entityClass) {
        return count(entityClass) > 0;
    }

    public boolean exists(Class<?> entityClass, String query, Object... params) {
        return count(entityClass, query, params) > 0;
    }

    public boolean exists(Class<?> entityClass, String query, Map<String, Object> params) {
        return count(entityClass, query, params) > 0;
    }

    public boolean exists(Class<?> entityClass, String query, Parameters params) {
        return count(entityClass, query, params) > 0;
    }

    public long deleteAll(Class<?> entityClass) {
        return (long) getEntityManager(entityClass).createQuery("DELETE FROM " + getEntityName(entityClass)).executeUpdate();
    }

    public boolean deleteById(Class<?> entityClass, Object id) {
        // Impl note : we load the entity then delete it because it's the only implementation generic enough for any model,
        // and correct in all cases (composite key, graph of entities, ...). HQL cannot be directly used for these reasons.
        Object entity = findById(entityClass, id);
        if (entity == null) {
            return false;
        }
        getEntityManager(entityClass).remove(entity);
        return true;
    }

    public long delete(Class<?> entityClass, String query, Object... params) {
        return bindParameters(
                getEntityManager(entityClass).createQuery(createDeleteQuery(entityClass, query, paramCount(params))), params)
                        .executeUpdate();
    }

    public long delete(Class<?> entityClass, String query, Map<String, Object> params) {
        return bindParameters(
                getEntityManager(entityClass).createQuery(createDeleteQuery(entityClass, query, paramCount(params))), params)
                        .executeUpdate();
    }

    public long delete(Class<?> entityClass, String query, Parameters params) {
        return delete(entityClass, query, params.map());
    }

    public IllegalStateException implementationInjectionMissing() {
        return new IllegalStateException(
                "This method is normally automatically overridden in subclasses: did you forget to annotate your entity with @Entity?");
    }

    /**
     * Execute update on default persistence unit
     */
    public int executeUpdate(String query, Object... params) {
        Query jpaQuery = getEntityManager(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME).createQuery(query);
        bindParameters(jpaQuery, params);
        return jpaQuery.executeUpdate();
    }

    /**
     * Execute update on default persistence unit
     */
    public int executeUpdate(String query, Map<String, Object> params) {
        Query jpaQuery = getEntityManager(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME).createQuery(query);
        bindParameters(jpaQuery, params);
        return jpaQuery.executeUpdate();
    }

    public int executeUpdate(String query, Class<?> entityClass, Object... params) {
        Query jpaQuery = getEntityManager(entityClass).createQuery(query);
        bindParameters(jpaQuery, params);
        return jpaQuery.executeUpdate();
    }

    public int executeUpdate(String query, Class<?> entityClass, Map<String, Object> params) {
        Query jpaQuery = getEntityManager(entityClass).createQuery(query);
        bindParameters(jpaQuery, params);
        return jpaQuery.executeUpdate();
    }

    public int executeUpdate(Class<?> entityClass, String query, Object... params) {
        String updateQuery = createUpdateQuery(entityClass, query, paramCount(params));
        return executeUpdate(updateQuery, entityClass, params);
    }

    public int executeUpdate(Class<?> entityClass, String query, Map<String, Object> params) {
        String updateQuery = createUpdateQuery(entityClass, query, paramCount(params));
        return executeUpdate(updateQuery, entityClass, params);
    }

    public int update(Class<?> entityClass, String query, Map<String, Object> params) {
        return executeUpdate(entityClass, query, params);
    }

    public int update(Class<?> entityClass, String query, Parameters params) {
        return update(entityClass, query, params.map());
    }

    public int update(Class<?> entityClass, String query, Object... params) {
        return executeUpdate(entityClass, query, params);
    }

    public void setRollbackOnly() {
        try {
            getTransactionManager().setRollbackOnly();
        } catch (SystemException e) {
            throw new IllegalStateException(e);
        }
    }
}
