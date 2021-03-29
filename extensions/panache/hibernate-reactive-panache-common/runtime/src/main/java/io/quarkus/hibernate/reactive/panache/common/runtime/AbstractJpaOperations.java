package io.quarkus.hibernate.reactive.panache.common.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.persistence.LockModeType;
import javax.persistence.PersistenceException;

import org.hibernate.internal.util.LockModeConverter;
import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.arc.Arc;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.hibernate.common.runtime.PanacheJpaUtil;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

public abstract class AbstractJpaOperations<PanacheQueryType> {

    protected abstract PanacheQueryType createPanacheQuery(Mutiny.Session session, String query, String orderBy,
            Object paramsArrayOrMap);

    protected abstract Uni<List<?>> list(PanacheQueryType query);

    protected abstract Multi<?> stream(PanacheQueryType query);

    //
    // Instance methods

    public Uni<Void> persist(Object entity) {
        return persist(getSession(), entity);
    }

    public Uni<Void> persist(Mutiny.Session session, Object entity) {
        if (!session.contains(entity)) {
            return session.persist(entity).map(v -> null);
        }
        return Uni.createFrom().nullItem();
    }

    public Uni<Void> persist(Iterable<?> entities) {
        return persist(StreamSupport.stream(entities.spliterator(), false));
    }

    public Uni<Void> persist(Object firstEntity, Object... entities) {
        List<Object> array = new ArrayList<>(entities.length + 1);
        array.add(firstEntity);
        for (Object entity : entities) {
            array.add(entity);
        }
        return persist(array.stream());
    }

    public Uni<Void> persist(Stream<?> entities) {
        Mutiny.Session session = getSession();
        List<Uni<Void>> uniList = entities.map(entity -> persist(session, entity)).collect(Collectors.toList());
        return Uni.combine().all().unis(uniList).discardItems();
        // this should work, but doesn't
        //        return Multi.createFrom().items(entities)
        //                .map(entity -> persist(session, entity))
        //                .onItem().ignoreAsUni();
    }

    public Uni<Void> delete(Object entity) {
        return getSession().remove(entity).map(v -> null);
    }

    public boolean isPersistent(Object entity) {
        return getSession().contains(entity);
    }

    public Uni<Void> flush() {
        return getSession().flush().map(v -> null);
    }

    //
    // Private stuff

    public static Mutiny.Session getSession() {
        Mutiny.Session session = Arc.container().instance(Mutiny.Session.class).get();
        // FIXME: handle null or exception?
        if (session == null) {
            throw new PersistenceException("No Mutiny.Session found. Do you have any JPA entities defined?");
        }
        return session;
    }

    public static Mutiny.Query<?> bindParameters(Mutiny.Query<?> query, Object[] params) {
        if (params == null || params.length == 0)
            return query;
        for (int i = 0; i < params.length; i++) {
            query.setParameter(i + 1, params[i]);
        }
        return query;
    }

    public static Mutiny.Query<?> bindParameters(Mutiny.Query<?> query, Map<String, Object> params) {
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

    public Uni<?> findById(Class<?> entityClass, Object id) {
        return getSession().find(entityClass, id);
    }

    public Uni<?> findById(Class<?> entityClass, Object id, LockModeType lockModeType) {
        return getSession().find(entityClass, id, LockModeConverter.convertToLockMode(lockModeType));
    }

    public PanacheQueryType find(Class<?> entityClass, String query, Object... params) {
        return find(entityClass, query, null, params);
    }

    public PanacheQueryType find(Class<?> entityClass, String query, Sort sort, Object... params) {
        String findQuery = PanacheJpaUtil.createFindQuery(entityClass, query, paramCount(params));
        Mutiny.Session session = getSession();
        // FIXME: check for duplicate ORDER BY clause?
        if (PanacheJpaUtil.isNamedQuery(query)) {
            String namedQuery = query.substring(1);
            NamedQueryUtil.checkNamedQuery(entityClass, namedQuery);
            return createPanacheQuery(session, query, PanacheJpaUtil.toOrderBy(sort), params);
        }
        return createPanacheQuery(session, findQuery, PanacheJpaUtil.toOrderBy(sort), params);
    }

    public PanacheQueryType find(Class<?> entityClass, String query, Map<String, Object> params) {
        return find(entityClass, query, null, params);
    }

    public PanacheQueryType find(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        String findQuery = PanacheJpaUtil.createFindQuery(entityClass, query, paramCount(params));
        Mutiny.Session session = getSession();
        // FIXME: check for duplicate ORDER BY clause?
        if (PanacheJpaUtil.isNamedQuery(query)) {
            String namedQuery = query.substring(1);
            NamedQueryUtil.checkNamedQuery(entityClass, namedQuery);
            return createPanacheQuery(session, query, PanacheJpaUtil.toOrderBy(sort), params);
        }
        return createPanacheQuery(session, findQuery, PanacheJpaUtil.toOrderBy(sort), params);
    }

    public PanacheQueryType find(Class<?> entityClass, String query, Parameters params) {
        return find(entityClass, query, null, params);
    }

    public PanacheQueryType find(Class<?> entityClass, String query, Sort sort, Parameters params) {
        return find(entityClass, query, sort, params.map());
    }

    public Uni<List<?>> list(Class<?> entityClass, String query, Object... params) {
        return list(find(entityClass, query, params));
    }

    public Uni<List<?>> list(Class<?> entityClass, String query, Sort sort, Object... params) {
        return list(find(entityClass, query, sort, params));
    }

    public Uni<List<?>> list(Class<?> entityClass, String query, Map<String, Object> params) {
        return list(find(entityClass, query, params));
    }

    public Uni<List<?>> list(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        return list(find(entityClass, query, sort, params));
    }

    public Uni<List<?>> list(Class<?> entityClass, String query, Parameters params) {
        return list(find(entityClass, query, params));
    }

    public Uni<List<?>> list(Class<?> entityClass, String query, Sort sort, Parameters params) {
        return list(find(entityClass, query, sort, params));
    }

    public Multi<?> stream(Class<?> entityClass, String query, Object... params) {
        return stream(find(entityClass, query, params));
    }

    public Multi<?> stream(Class<?> entityClass, String query, Sort sort, Object... params) {
        return stream(find(entityClass, query, sort, params));
    }

    public Multi<?> stream(Class<?> entityClass, String query, Map<String, Object> params) {
        return stream(find(entityClass, query, params));
    }

    public Multi<?> stream(Class<?> entityClass, String query, Sort sort, Map<String, Object> params) {
        return stream(find(entityClass, query, sort, params));
    }

    public Multi<?> stream(Class<?> entityClass, String query, Parameters params) {
        return stream(find(entityClass, query, params));
    }

    public Multi<?> stream(Class<?> entityClass, String query, Sort sort, Parameters params) {
        return stream(find(entityClass, query, sort, params));
    }

    public PanacheQueryType findAll(Class<?> entityClass) {
        String query = "FROM " + PanacheJpaUtil.getEntityName(entityClass);
        Mutiny.Session session = getSession();
        return createPanacheQuery(session, query, null, null);
    }

    public PanacheQueryType findAll(Class<?> entityClass, Sort sort) {
        String query = "FROM " + PanacheJpaUtil.getEntityName(entityClass);
        Mutiny.Session session = getSession();
        return createPanacheQuery(session, query, PanacheJpaUtil.toOrderBy(sort), null);
    }

    public Uni<List<?>> listAll(Class<?> entityClass) {
        return list(findAll(entityClass));
    }

    public Uni<List<?>> listAll(Class<?> entityClass, Sort sort) {
        return list(findAll(entityClass, sort));
    }

    public Multi<?> streamAll(Class<?> entityClass) {
        return stream(findAll(entityClass));
    }

    public Multi<?> streamAll(Class<?> entityClass, Sort sort) {
        return stream(findAll(entityClass, sort));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Uni<Long> count(Class<?> entityClass) {
        return (Uni) getSession().createQuery("SELECT COUNT(*) FROM " + PanacheJpaUtil.getEntityName(entityClass))
                .getSingleResult();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Uni<Long> count(Class<?> entityClass, String query, Object... params) {
        return (Uni) bindParameters(
                getSession().createQuery(PanacheJpaUtil.createCountQuery(entityClass, query, paramCount(params))),
                params).getSingleResult();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Uni<Long> count(Class<?> entityClass, String query, Map<String, Object> params) {
        return (Uni) bindParameters(
                getSession().createQuery(PanacheJpaUtil.createCountQuery(entityClass, query, paramCount(params))),
                params).getSingleResult();
    }

    public Uni<Long> count(Class<?> entityClass, String query, Parameters params) {
        return count(entityClass, query, params.map());
    }

    public Uni<Boolean> exists(Class<?> entityClass) {
        return count(entityClass).map(c -> c > 0);
    }

    public Uni<Boolean> exists(Class<?> entityClass, String query, Object... params) {
        return count(entityClass, query, params).map(c -> c > 0);
    }

    public Uni<Boolean> exists(Class<?> entityClass, String query, Map<String, Object> params) {
        return count(entityClass, query, params).map(c -> c > 0);
    }

    public Uni<Boolean> exists(Class<?> entityClass, String query, Parameters params) {
        return count(entityClass, query, params).map(c -> c > 0);
    }

    public Uni<Long> deleteAll(Class<?> entityClass) {
        return getSession().createQuery("DELETE FROM " + PanacheJpaUtil.getEntityName(entityClass)).executeUpdate()
                .map(i -> i.longValue());
    }

    public Uni<Boolean> deleteById(Class<?> entityClass, Object id) {
        // Impl note : we load the entity then delete it because it's the only implementation generic enough for any model,
        // and correct in all cases (composite key, graph of entities, ...). HQL cannot be directly used for these reasons.
        return findById(entityClass, id)
                .flatMap(entity -> {
                    if (entity == null) {
                        return Uni.createFrom().item(false);
                    }
                    return getSession().remove(entity).map(v -> true);
                });
    }

    public Uni<Long> delete(Class<?> entityClass, String query, Object... params) {
        return bindParameters(
                getSession().createQuery(PanacheJpaUtil.createDeleteQuery(entityClass, query, paramCount(params))), params)
                        .executeUpdate().map(i -> i.longValue());
    }

    public Uni<Long> delete(Class<?> entityClass, String query, Map<String, Object> params) {
        return bindParameters(
                getSession().createQuery(PanacheJpaUtil.createDeleteQuery(entityClass, query, paramCount(params))), params)
                        .executeUpdate().map(i -> i.longValue());
    }

    public Uni<Long> delete(Class<?> entityClass, String query, Parameters params) {
        return delete(entityClass, query, params.map());
    }

    public IllegalStateException implementationInjectionMissing() {
        return new IllegalStateException(
                "This method is normally automatically overridden in subclasses: did you forget to annotate your entity with @Entity?");
    }

    public static Uni<Integer> executeUpdate(String query, Object... params) {
        Mutiny.Query<?> jpaQuery = getSession().createQuery(query);
        bindParameters(jpaQuery, params);
        return jpaQuery.executeUpdate();
    }

    public static Uni<Integer> executeUpdate(String query, Map<String, Object> params) {
        Mutiny.Query<?> jpaQuery = getSession().createQuery(query);
        bindParameters(jpaQuery, params);
        return jpaQuery.executeUpdate();
    }

    public Uni<Integer> executeUpdate(Class<?> entityClass, String query, Object... params) {
        String updateQuery = PanacheJpaUtil.createUpdateQuery(entityClass, query, paramCount(params));
        return executeUpdate(updateQuery, params);
    }

    public Uni<Integer> executeUpdate(Class<?> entityClass, String query, Map<String, Object> params) {
        String updateQuery = PanacheJpaUtil.createUpdateQuery(entityClass, query, paramCount(params));
        return executeUpdate(updateQuery, params);
    }

    public Uni<Integer> update(Class<?> entityClass, String query, Map<String, Object> params) {
        return executeUpdate(entityClass, query, params);
    }

    public Uni<Integer> update(Class<?> entityClass, String query, Parameters params) {
        return update(entityClass, query, params.map());
    }

    public Uni<Integer> update(Class<?> entityClass, String query, Object... params) {
        return executeUpdate(entityClass, query, params);
    }
}
