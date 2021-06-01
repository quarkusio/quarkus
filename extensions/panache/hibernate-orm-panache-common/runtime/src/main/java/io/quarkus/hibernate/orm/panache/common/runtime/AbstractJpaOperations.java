package io.quarkus.hibernate.orm.panache.common.runtime;

import static io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;

import java.util.Collections;
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

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.hibernate.common.runtime.PanacheJpaUtil;

public abstract class AbstractJpaOperations<PanacheQueryType> {
    private static volatile Map<String, String> entityToPersistenceUnit = Collections.emptyMap();

    public static void setEntityToPersistenceUnit(Map<String, String> map) {
        entityToPersistenceUnit = Collections.unmodifiableMap(map);
    }

    protected abstract PanacheQueryType createPanacheQuery(EntityManager em, String query, String orderBy,
            Object paramsArrayOrMap);

    public abstract List<?> list(PanacheQueryType query);

    public abstract Stream<?> stream(PanacheQueryType query);

    /**
     * Returns the {@link EntityManager} for the given {@link Class<?> entity}
     *
     * @return {@link EntityManager}
     */
    public EntityManager getEntityManager(Class<?> clazz) {
        String clazzName = clazz.getName();
        String persistentUnitName = entityToPersistenceUnit.get(clazzName);
        return getEntityManager(persistentUnitName);
    }

    public EntityManager getEntityManager(String persistentUnitName) {
        ArcContainer arcContainer = Arc.container();
        if (persistentUnitName == null || PersistenceUnitUtil.isDefaultPersistenceUnit(persistentUnitName)) {
            InstanceHandle<EntityManager> emHandle = arcContainer.instance(EntityManager.class);
            if (emHandle.isAvailable()) {
                return emHandle.get();
            }
            if (!arcContainer.instance(AgroalDataSource.class).isAvailable()) {
                throw new IllegalStateException(
                        "The default datasource has not been properly configured. See https://quarkus.io/guides/datasource#jdbc-datasource for information on how to do that.");
            }
            throw new IllegalStateException(
                    "No entities were found. Did you forget to annotate your Panache Entity classes with '@Entity'?");
        }

        InstanceHandle<EntityManager> emHandle = arcContainer.instance(EntityManager.class,
                new PersistenceUnit.PersistenceUnitLiteral(persistentUnitName));
        if (emHandle.isAvailable()) {
            return emHandle.get();
        }
        if (!arcContainer.instance(AgroalDataSource.class,
                new DataSource.DataSourceLiteral(persistentUnitName)).isAvailable()) {
            throw new IllegalStateException(
                    "The named datasource '" + persistentUnitName
                            + "' has not been properly configured. See https://quarkus.io/guides/datasource#multiple-datasources for information on how to do that.");
        }
        throw new IllegalStateException(
                "No entities were attached to persistence unit '" + persistentUnitName
                        + "'. Did you forget to annotate your Panache Entity classes with '@Entity' or improperly configure the 'quarkus.hibernate-orm.\" "
                        + persistentUnitName + "\".packages' property?");
    }

    public EntityManager getEntityManager() {
        return getEntityManager(DEFAULT_PERSISTENCE_UNIT_NAME);
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

    public void flush() {
        getEntityManager().flush();
    }

    public void flush(Object entity) {
        getEntityManager(entity.getClass()).flush();
    }

    public void flush(Class<?> clazz) {
        getEntityManager(clazz).flush();
    }
    //
    // Private stuff

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

    public int paramCount(Object[] params) {
        return params != null ? params.length : 0;
    }

    public int paramCount(Map<String, Object> params) {
        return params != null ? params.size() : 0;
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
        String findQuery = PanacheJpaUtil.createFindQuery(entityClass, query, paramCount(params));
        EntityManager em = getEntityManager(entityClass);
        // FIXME: check for duplicate ORDER BY clause?
        if (PanacheJpaUtil.isNamedQuery(query)) {
            String namedQuery = query.substring(1);
            NamedQueryUtil.checkNamedQuery(entityClass, namedQuery);
            return createPanacheQuery(em, query, PanacheJpaUtil.toOrderBy(sort), params);
        }
        return createPanacheQuery(em, findQuery, PanacheJpaUtil.toOrderBy(sort), params);
    }

    public PanacheQueryType find(Class<?> entityClass, String query, Map<String, Object> params) {
        return find(entityClass, query, null, params);
    }

    public PanacheQueryType find(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        String findQuery = PanacheJpaUtil.createFindQuery(entityClass, query, paramCount(params));
        EntityManager em = getEntityManager(entityClass);
        // FIXME: check for duplicate ORDER BY clause?
        if (PanacheJpaUtil.isNamedQuery(query)) {
            String namedQuery = query.substring(1);
            NamedQueryUtil.checkNamedQuery(entityClass, namedQuery);
            return createPanacheQuery(em, query, PanacheJpaUtil.toOrderBy(sort), params);
        }
        return createPanacheQuery(em, findQuery, PanacheJpaUtil.toOrderBy(sort), params);
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
        String query = "FROM " + PanacheJpaUtil.getEntityName(entityClass);
        EntityManager em = getEntityManager(entityClass);
        return createPanacheQuery(em, query, null, null);
    }

    public PanacheQueryType findAll(Class<?> entityClass, Sort sort) {
        String query = "FROM " + PanacheJpaUtil.getEntityName(entityClass);
        EntityManager em = getEntityManager(entityClass);
        return createPanacheQuery(em, query, PanacheJpaUtil.toOrderBy(sort), null);
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
        return (long) getEntityManager(entityClass)
                .createQuery("SELECT COUNT(*) FROM " + PanacheJpaUtil.getEntityName(entityClass))
                .getSingleResult();
    }

    public long count(Class<?> entityClass, String query, Object... params) {
        return (long) bindParameters(
                getEntityManager(entityClass)
                        .createQuery(PanacheJpaUtil.createCountQuery(entityClass, query, paramCount(params))),
                params).getSingleResult();
    }

    public long count(Class<?> entityClass, String query, Map<String, Object> params) {
        return (long) bindParameters(
                getEntityManager(entityClass)
                        .createQuery(PanacheJpaUtil.createCountQuery(entityClass, query, paramCount(params))),
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
        return getEntityManager(entityClass).createQuery("DELETE FROM " + PanacheJpaUtil.getEntityName(entityClass))
                .executeUpdate();
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
                getEntityManager(entityClass)
                        .createQuery(PanacheJpaUtil.createDeleteQuery(entityClass, query, paramCount(params))),
                params)
                        .executeUpdate();
    }

    public long delete(Class<?> entityClass, String query, Map<String, Object> params) {
        return bindParameters(
                getEntityManager(entityClass)
                        .createQuery(PanacheJpaUtil.createDeleteQuery(entityClass, query, paramCount(params))),
                params)
                        .executeUpdate();
    }

    public long delete(Class<?> entityClass, String query, Parameters params) {
        return delete(entityClass, query, params.map());
    }

    public static IllegalStateException implementationInjectionMissing() {
        return new IllegalStateException(
                "This method is normally automatically overridden in subclasses: did you forget to annotate your entity with @Entity?");
    }

    /**
     * Execute update on default persistence unit
     */
    public int executeUpdate(String query, Object... params) {
        Query jpaQuery = getEntityManager(DEFAULT_PERSISTENCE_UNIT_NAME).createQuery(query);
        bindParameters(jpaQuery, params);
        return jpaQuery.executeUpdate();
    }

    /**
     * Execute update on default persistence unit
     */
    public int executeUpdate(String query, Map<String, Object> params) {
        Query jpaQuery = getEntityManager(DEFAULT_PERSISTENCE_UNIT_NAME).createQuery(query);
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
        String updateQuery = PanacheJpaUtil.createUpdateQuery(entityClass, query, paramCount(params));
        return executeUpdate(updateQuery, entityClass, params);
    }

    public int executeUpdate(Class<?> entityClass, String query, Map<String, Object> params) {
        String updateQuery = PanacheJpaUtil.createUpdateQuery(entityClass, query, paramCount(params));
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

    public static void setRollbackOnly() {
        try {
            getTransactionManager().setRollbackOnly();
        } catch (SystemException e) {
            throw new IllegalStateException(e);
        }
    }
}
