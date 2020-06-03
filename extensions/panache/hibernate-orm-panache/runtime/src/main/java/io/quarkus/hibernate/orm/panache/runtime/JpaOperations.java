package io.quarkus.hibernate.orm.panache.runtime;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.transaction.TransactionManager;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.hibernate.common.runtime.PanacheJpaUtil;

public class JpaOperations {

    private static class JavaJpaOperations extends AbstractJpaOperations<PanacheQueryImpl<?>> {

        @Override
        protected PanacheQueryImpl<?> createPanacheQuery(EntityManager em, String query, String orderBy,
                Object paramsArrayOrMap) {
            return new PanacheQueryImpl<>(em, query, orderBy, paramsArrayOrMap);
        }

        @Override
        protected List<?> list(PanacheQueryImpl<?> query) {
            return query.list();
        }

        @Override
        protected Stream<?> stream(PanacheQueryImpl<?> query) {
            return query.stream();
        }

        @Override
        public EntityManager getEntityManager(Class<?> clazz) {
            String clazzName = clazz.getName();
            String persistentUnitName = entityToPersistenceUnit.get(clazzName);
            return super.getEntityManager(persistentUnitName);
        }
    }

    private static final JavaJpaOperations delegate = new JavaJpaOperations();

    private static volatile Map<String, String> entityToPersistenceUnit = Collections.emptyMap();

    static void setEntityToPersistenceUnit(Map<String, String> entityToPersistenceUnit) {
        JpaOperations.entityToPersistenceUnit = Collections.unmodifiableMap(entityToPersistenceUnit);
    }

    //
    // Instance methods
    public static void persist(Object entity) {
        delegate.persist(entity);
    }

    public static void persist(EntityManager em, Object entity) {
        delegate.persist(em, entity);
    }

    public static void persist(Iterable<?> entities) {
        delegate.persist(entities);
    }

    public static void persist(Object firstEntity, Object... entities) {
        delegate.persist(firstEntity, entities);
    }

    public static void persist(Stream<?> entities) {
        delegate.persist(entities);
    }

    public static void delete(Object entity) {
        delegate.delete(entity);
    }

    public static boolean isPersistent(Object entity) {
        return delegate.isPersistent(entity);
    }

    /**
     * Flushes the default to the database using the default EntityManager.
     */
    public static void flush() {
        delegate.getEntityManager().flush();
    }

    /**
     * Flushes all pending changes to the database using entity's EntityManager.
     */
    public static void flush(Object entity) {
        delegate.flush(entity);
    }

    /**
     * Default entity manager
     */
    public static EntityManager getEntityManager() {
        return delegate.getEntityManager();
    }

    public static EntityManager getEntityManager(Class<?> clazz) {
        return delegate.getEntityManager(clazz);
    }

    public static EntityManager getEntityManager(String persistentUnitName) {
        return delegate.getEntityManager(persistentUnitName);
    }

    public static TransactionManager getTransactionManager() {
        return delegate.getTransactionManager();
    }

    public static Query bindParameters(Query query, Object[] params) {
        return AbstractJpaOperations.bindParameters(query, params);
    }

    public static Query bindParameters(Query query, Map<String, Object> params) {
        return AbstractJpaOperations.bindParameters(query, params);
    }

    static int paramCount(Object[] params) {
        return delegate.paramCount(params);
    }

    static int paramCount(Map<String, Object> params) {
        return delegate.paramCount(params);
    }

    //
    //    private static String getEntityName(Class<?> entityClass) {
    //        return delegate.getEntityName(entityClass);
    //    }
    //
    static String createFindQuery(Class<?> entityClass, String query, int paramCount) {
        return PanacheJpaUtil.createFindQuery(entityClass, query, paramCount);
    }
    //
    //    static boolean isNamedQuery(String query) {
    //        return delegate.isNamedQuery(query);
    //    }
    //
    //    private static String createCountQuery(Class<?> entityClass, String query, int paramCount) {
    //        return delegate.createCountQuery(entityClass, query, paramCount);
    //    }
    //
    //    private static String createUpdateQuery(Class<?> entityClass, String query, int paramCount) {
    //        return delegate.createUpdateQuery(entityClass, query, paramCount);
    //    }
    //
    //    private static String createDeleteQuery(Class<?> entityClass, String query, int paramCount) {
    //        return delegate.createDeleteQuery(entityClass, query, paramCount);
    //    }

    public static String toOrderBy(Sort sort) {
        return PanacheJpaUtil.toOrderBy(sort);
    }

    //
    // Queries

    public static Object findById(Class<?> entityClass, Object id) {
        return delegate.findById(entityClass, id);
    }

    public static Object findById(Class<?> entityClass, Object id, LockModeType lockModeType) {
        return delegate.findById(entityClass, id, lockModeType);
    }

    public static Optional<?> findByIdOptional(Class<?> entityClass, Object id) {
        return delegate.findByIdOptional(entityClass, id);
    }

    public static Optional<?> findByIdOptional(Class<?> entityClass, Object id, LockModeType lockModeType) {
        return delegate.findByIdOptional(entityClass, id, lockModeType);
    }

    public static PanacheQuery<?> find(Class<?> entityClass, String query, Object... params) {
        return delegate.find(entityClass, query, params);
    }

    public static PanacheQuery<?> find(Class<?> entityClass, String query, Sort sort, Object... params) {
        return delegate.find(entityClass, query, sort, params);
    }

    public static PanacheQuery<?> find(Class<?> entityClass, String query, Map<String, Object> params) {
        return delegate.find(entityClass, query, params);
    }

    public static PanacheQuery<?> find(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        return delegate.find(entityClass, query, sort, params);
    }

    public static PanacheQuery<?> find(Class<?> entityClass, String query, Parameters params) {
        return delegate.find(entityClass, query, params);
    }

    public static PanacheQuery<?> find(Class<?> entityClass, String query, Sort sort, Parameters params) {
        return delegate.find(entityClass, query, sort, params);
    }

    public static List<?> list(Class<?> entityClass, String query, Object... params) {
        return delegate.list(entityClass, query, params);
    }

    public static List<?> list(Class<?> entityClass, String query, Sort sort, Object... params) {
        return delegate.list(entityClass, query, sort, params);
    }

    public static List<?> list(Class<?> entityClass, String query, Map<String, Object> params) {
        return delegate.list(entityClass, query, params);
    }

    public static List<?> list(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        return delegate.list(entityClass, query, sort, params);
    }

    public static List<?> list(Class<?> entityClass, String query, Parameters params) {
        return delegate.list(entityClass, query, params);
    }

    public static List<?> list(Class<?> entityClass, String query, Sort sort, Parameters params) {
        return delegate.list(entityClass, query, sort, params);
    }

    public static Stream<?> stream(Class<?> entityClass, String query, Object... params) {
        return delegate.stream(entityClass, query, params);
    }

    public static Stream<?> stream(Class<?> entityClass, String query, Sort sort, Object... params) {
        return delegate.stream(entityClass, query, sort, params);
    }

    public static Stream<?> stream(Class<?> entityClass, String query, Map<String, Object> params) {
        return delegate.stream(entityClass, query, params);
    }

    public static Stream<?> stream(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        return delegate.stream(entityClass, query, sort, params);
    }

    public static Stream<?> stream(Class<?> entityClass, String query, Parameters params) {
        return delegate.stream(entityClass, query, params);
    }

    public static Stream<?> stream(Class<?> entityClass, String query, Sort sort, Parameters params) {
        return delegate.stream(entityClass, query, sort, params);
    }

    public static PanacheQuery<?> findAll(Class<?> entityClass) {
        return delegate.findAll(entityClass);
    }

    public static PanacheQuery<?> findAll(Class<?> entityClass, Sort sort) {
        return delegate.findAll(entityClass, sort);
    }

    public static List<?> listAll(Class<?> entityClass) {
        return delegate.listAll(entityClass);
    }

    public static List<?> listAll(Class<?> entityClass, Sort sort) {
        return delegate.listAll(entityClass, sort);
    }

    public static Stream<?> streamAll(Class<?> entityClass) {
        return delegate.streamAll(entityClass);
    }

    public static Stream<?> streamAll(Class<?> entityClass, Sort sort) {
        return delegate.streamAll(entityClass, sort);
    }

    public static long count(Class<?> entityClass) {
        return delegate.count(entityClass);
    }

    public static long count(Class<?> entityClass, String query, Object... params) {
        return delegate.count(entityClass, query, params);
    }

    public static long count(Class<?> entityClass, String query, Map<String, Object> params) {
        return delegate.count(entityClass, query, params);
    }

    public static long count(Class<?> entityClass, String query, Parameters params) {
        return delegate.count(entityClass, query, params);
    }

    public static boolean exists(Class<?> entityClass) {
        return delegate.exists(entityClass);
    }

    public static boolean exists(Class<?> entityClass, String query, Object... params) {
        return delegate.exists(entityClass, query, params);
    }

    public static boolean exists(Class<?> entityClass, String query, Map<String, Object> params) {
        return delegate.exists(entityClass, query, params);
    }

    public static boolean exists(Class<?> entityClass, String query, Parameters params) {
        return delegate.exists(entityClass, query, params);
    }

    public static long deleteAll(Class<?> entityClass) {
        return delegate.deleteAll(entityClass);
    }

    public static boolean deleteById(Class<?> entityClass, Object id) {
        return delegate.deleteById(entityClass, id);
    }

    public static long delete(Class<?> entityClass, String query, Object... params) {
        return delegate.delete(entityClass, query, params);
    }

    public static long delete(Class<?> entityClass, String query, Map<String, Object> params) {
        return delegate.delete(entityClass, query, params);
    }

    public static long delete(Class<?> entityClass, String query, Parameters params) {
        return delegate.delete(entityClass, query, params);
    }

    public static IllegalStateException implementationInjectionMissing() {
        return delegate.implementationInjectionMissing();
    }

    public static int executeUpdate(String query, Object... params) {
        return delegate.executeUpdate(query, params);
    }

    public static int executeUpdate(String query, Map<String, Object> params) {
        return delegate.executeUpdate(query, params);
    }

    public static int executeUpdate(Class<?> entityClass, String query, Object... params) {
        return delegate.executeUpdate(entityClass, query, params);
    }

    public static int executeUpdate(Class<?> entityClass, String query, Map<String, Object> params) {
        return delegate.executeUpdate(entityClass, query, params);
    }

    public static int update(Class<?> entityClass, String query, Map<String, Object> params) {
        return delegate.update(entityClass, query, params);
    }

    public static int update(Class<?> entityClass, String query, Parameters params) {
        return delegate.update(entityClass, query, params);
    }

    public static int update(Class<?> entityClass, String query, Object... params) {
        return delegate.update(entityClass, query, params);
    }

    public static void setRollbackOnly() {
        delegate.setRollbackOnly();
    }

}
