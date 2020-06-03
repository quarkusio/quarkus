package io.quarkus.hibernate.orm.panache.kotlin.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.transaction.TransactionManager;

import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations;
import io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;

public class JpaOperations {

    private static final KotlinJpaOperations delegate = new KotlinJpaOperations();

    public static Query bindParameters(Query query, Object[] params) {
        return AbstractJpaOperations.bindParameters(query, params);
    }

    //
    // Instance methods

    public static Query bindParameters(Query query, Map<String, Object> params) {
        return AbstractJpaOperations.bindParameters(query, params);
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

    static String createFindQuery(Class<?> entityClass, String query, int paramCount) {
        return delegate.createFindQuery(entityClass, query, paramCount);
    }

    public static void delete(Object entity) {
        delegate.delete(entity);
    }

    public static long delete(Class<?> entityClass, String query, Object... params) {
        return delegate.delete(entityClass, query, params);
    }

    //
    // Private stuff

    public static long delete(Class<?> entityClass, String query, Map<String, Object> params) {
        return delegate.delete(entityClass, query, params);
    }

    public static long delete(Class<?> entityClass, String query, Parameters params) {
        return delegate.delete(entityClass, query, params);
    }

    public static long deleteAll(Class<?> entityClass) {
        return delegate.deleteAll(entityClass);
    }

    public static boolean deleteById(Class<?> entityClass, Object id) {
        return delegate.deleteById(entityClass, id);
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

    public static PanacheQuery<?> findAll(Class<?> entityClass) {
        return delegate.findAll(entityClass);
    }

    public static PanacheQuery<?> findAll(Class<?> entityClass, Sort sort) {
        return delegate.findAll(entityClass, sort);
    }

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

    public static void flush() {
        delegate.flush();
    }

    public static EntityManager getEntityManager() {
        return delegate.getEntityManager();
    }

    public static TransactionManager getTransactionManager() {
        return delegate.getTransactionManager();
    }

    public static IllegalStateException implementationInjectionMissing() {
        return delegate.implementationInjectionMissing();
    }

    public static boolean isPersistent(Object entity) {
        return delegate.isPersistent(entity);
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

    public static List<?> listAll(Class<?> entityClass) {
        return delegate.listAll(entityClass);
    }

    public static List<?> listAll(Class<?> entityClass, Sort sort) {
        return delegate.listAll(entityClass, sort);
    }

    public static void merge(Object entity) {
        delegate.merge(entity);
    }

    static int paramCount(Object[] params) {
        return delegate.paramCount(params);
    }

    static int paramCount(Map<String, Object> params) {
        return delegate.paramCount(params);
    }

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

    public static void setRollbackOnly() {
        delegate.setRollbackOnly();
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

    public static Stream<?> streamAll(Class<?> entityClass) {
        return delegate.streamAll(entityClass);
    }

    public static Stream<?> streamAll(Class<?> entityClass, Sort sort) {
        return delegate.streamAll(entityClass, sort);
    }

    public static String toOrderBy(Sort sort) {
        return delegate.toOrderBy(sort);
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

    private static class KotlinJpaOperations extends AbstractJpaOperations<PanacheQueryImpl<?>> {

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

    }

}
