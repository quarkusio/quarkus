package io.quarkus.hibernate.reactive.panache.runtime;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.persistence.LockModeType;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.quarkus.hibernate.reactive.panache.common.runtime.AbstractJpaOperations;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.hibernate.common.runtime.PanacheJpaUtil;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public class JpaOperations {

    private static class JavaJpaOperations extends AbstractJpaOperations<PanacheQueryImpl<?>> {

        @Override
        protected PanacheQueryImpl<?> createPanacheQuery(Mutiny.Session session, String query, String orderBy,
                Object paramsArrayOrMap) {
            return new PanacheQueryImpl<>(session, query, orderBy, paramsArrayOrMap);
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        protected Uni<List<?>> list(PanacheQueryImpl<?> query) {
            return (Uni) query.list();
        }

        @Override
        protected Multi<?> stream(PanacheQueryImpl<?> query) {
            return query.stream();
        }

    }

    private static final JavaJpaOperations delegate = new JavaJpaOperations();

    //
    // Instance methods

    public static Uni<Void> persist(Object entity) {
        return delegate.persist(entity);
    }

    public static Uni<Void> persist(Mutiny.Session session, Object entity) {
        return delegate.persist(session, entity);
    }

    public static Uni<Void> persist(Iterable<?> entities) {
        return delegate.persist(entities);
    }

    public static Uni<Void> persist(Object firstEntity, Object... entities) {
        return delegate.persist(firstEntity, entities);
    }

    public static Uni<Void> persist(Stream<?> entities) {
        return delegate.persist(entities);
    }

    public static Uni<Void> delete(Object entity) {
        return delegate.delete(entity);
    }

    public static boolean isPersistent(Object entity) {
        return delegate.isPersistent(entity);
    }

    public static Uni<Void> flush() {
        return delegate.flush();
    }

    //
    // Private stuff

    public static Mutiny.Session getSession() {
        return delegate.getSession();
    }

    public static Mutiny.Query<?> bindParameters(Mutiny.Query<?> query, Object[] params) {
        return AbstractJpaOperations.bindParameters(query, params);
    }

    public static Mutiny.Query<?> bindParameters(Mutiny.Query<?> query, Map<String, Object> params) {
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

    public static Uni<?> findById(Class<?> entityClass, Object id) {
        return delegate.findById(entityClass, id);
    }

    public static Uni<?> findById(Class<?> entityClass, Object id, LockModeType lockModeType) {
        return delegate.findById(entityClass, id, lockModeType);
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

    public static Uni<List<?>> list(Class<?> entityClass, String query, Object... params) {
        return delegate.list(entityClass, query, params);
    }

    public static Uni<List<?>> list(Class<?> entityClass, String query, Sort sort, Object... params) {
        return delegate.list(entityClass, query, sort, params);
    }

    public static Uni<List<?>> list(Class<?> entityClass, String query, Map<String, Object> params) {
        return delegate.list(entityClass, query, params);
    }

    public static Uni<List<?>> list(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        return delegate.list(entityClass, query, sort, params);
    }

    public static Uni<List<?>> list(Class<?> entityClass, String query, Parameters params) {
        return delegate.list(entityClass, query, params);
    }

    public static Uni<List<?>> list(Class<?> entityClass, String query, Sort sort, Parameters params) {
        return delegate.list(entityClass, query, sort, params);
    }

    public static Multi<?> stream(Class<?> entityClass, String query, Object... params) {
        return delegate.stream(entityClass, query, params);
    }

    public static Multi<?> stream(Class<?> entityClass, String query, Sort sort, Object... params) {
        return delegate.stream(entityClass, query, sort, params);
    }

    public static Multi<?> stream(Class<?> entityClass, String query, Map<String, Object> params) {
        return delegate.stream(entityClass, query, params);
    }

    public static Multi<?> stream(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        return delegate.stream(entityClass, query, sort, params);
    }

    public static Multi<?> stream(Class<?> entityClass, String query, Parameters params) {
        return delegate.stream(entityClass, query, params);
    }

    public static Multi<?> stream(Class<?> entityClass, String query, Sort sort, Parameters params) {
        return delegate.stream(entityClass, query, sort, params);
    }

    public static PanacheQuery<?> findAll(Class<?> entityClass) {
        return delegate.findAll(entityClass);
    }

    public static PanacheQuery<?> findAll(Class<?> entityClass, Sort sort) {
        return delegate.findAll(entityClass, sort);
    }

    public static Uni<List<?>> listAll(Class<?> entityClass) {
        return delegate.listAll(entityClass);
    }

    public static Uni<List<?>> listAll(Class<?> entityClass, Sort sort) {
        return delegate.listAll(entityClass, sort);
    }

    public static Multi<?> streamAll(Class<?> entityClass) {
        return delegate.streamAll(entityClass);
    }

    public static Multi<?> streamAll(Class<?> entityClass, Sort sort) {
        return delegate.streamAll(entityClass, sort);
    }

    public static Uni<Long> count(Class<?> entityClass) {
        return delegate.count(entityClass);
    }

    public static Uni<Long> count(Class<?> entityClass, String query, Object... params) {
        return delegate.count(entityClass, query, params);
    }

    public static Uni<Long> count(Class<?> entityClass, String query, Map<String, Object> params) {
        return delegate.count(entityClass, query, params);
    }

    public static Uni<Long> count(Class<?> entityClass, String query, Parameters params) {
        return delegate.count(entityClass, query, params);
    }

    public static Uni<Boolean> exists(Class<?> entityClass) {
        return delegate.exists(entityClass);
    }

    public static Uni<Boolean> exists(Class<?> entityClass, String query, Object... params) {
        return delegate.exists(entityClass, query, params);
    }

    public static Uni<Boolean> exists(Class<?> entityClass, String query, Map<String, Object> params) {
        return delegate.exists(entityClass, query, params);
    }

    public static Uni<Boolean> exists(Class<?> entityClass, String query, Parameters params) {
        return delegate.exists(entityClass, query, params);
    }

    public static Uni<Long> deleteAll(Class<?> entityClass) {
        return delegate.deleteAll(entityClass);
    }

    public static Uni<Boolean> deleteById(Class<?> entityClass, Object id) {
        return delegate.deleteById(entityClass, id);
    }

    public static Uni<Long> delete(Class<?> entityClass, String query, Object... params) {
        return delegate.delete(entityClass, query, params);
    }

    public static Uni<Long> delete(Class<?> entityClass, String query, Map<String, Object> params) {
        return delegate.delete(entityClass, query, params);
    }

    public static Uni<Long> delete(Class<?> entityClass, String query, Parameters params) {
        return delegate.delete(entityClass, query, params);
    }

    public static IllegalStateException implementationInjectionMissing() {
        return delegate.implementationInjectionMissing();
    }

    public static Uni<Integer> executeUpdate(String query, Object... params) {
        return delegate.executeUpdate(query, params);
    }

    public static Uni<Integer> executeUpdate(String query, Map<String, Object> params) {
        return delegate.executeUpdate(query, params);
    }

    public static Uni<Integer> executeUpdate(Class<?> entityClass, String query, Object... params) {
        return delegate.executeUpdate(entityClass, query, params);
    }

    public static Uni<Integer> executeUpdate(Class<?> entityClass, String query, Map<String, Object> params) {
        return delegate.executeUpdate(entityClass, query, params);
    }

    public static Uni<Integer> update(Class<?> entityClass, String query, Map<String, Object> params) {
        return delegate.update(entityClass, query, params);
    }

    public static Uni<Integer> update(Class<?> entityClass, String query, Parameters params) {
        return delegate.update(entityClass, query, params);
    }

    public static Uni<Integer> update(Class<?> entityClass, String query, Object... params) {
        return delegate.update(entityClass, query, params);
    }
}
