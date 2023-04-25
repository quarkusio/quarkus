package io.quarkus.hibernate.reactive.panache.common.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import jakarta.persistence.LockModeType;

import org.hibernate.internal.util.LockModeConverter;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.mutiny.Mutiny.Session;

import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.hibernate.common.runtime.PanacheJpaUtil;
import io.smallrye.mutiny.Uni;

public abstract class AbstractJpaOperations<PanacheQueryType> {

    // FIXME: make it configurable?
    static final long TIMEOUT_MS = 5000;
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    protected abstract PanacheQueryType createPanacheQuery(Uni<Mutiny.Session> session, String query, String orderBy,
            Object paramsArrayOrMap);

    protected abstract Uni<List<?>> list(PanacheQueryType query);

    //
    // Instance methods

    public Uni<Void> persist(Object entity) {
        return persist(getSession(), entity);
    }

    public Uni<Void> persist(Uni<Mutiny.Session> sessionUni, Object entity) {
        return sessionUni.chain(session -> {
            if (!session.contains(entity)) {
                return session.persist(entity);
            }
            return Uni.createFrom().nullItem();
        });
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Uni<Void> persist(Iterable<?> entities) {
        List list = new ArrayList();
        for (Object entity : entities) {
            list.add(entity);
        }
        return persist(list.toArray(EMPTY_OBJECT_ARRAY));
    }

    public Uni<Void> persist(Object firstEntity, Object... entities) {
        List<Object> list = new ArrayList<>(entities.length + 1);
        list.add(firstEntity);
        Collections.addAll(list, entities);
        return persist(list.toArray(EMPTY_OBJECT_ARRAY));
    }

    public Uni<Void> persist(Stream<?> entities) {
        return persist(entities.toArray());
    }

    public Uni<Void> persist(Object... entities) {
        return getSession().chain(session -> session.persistAll(entities));
    }

    public Uni<Void> delete(Object entity) {
        return getSession().chain(session -> session.remove(entity));
    }

    public boolean isPersistent(Object entity) {
        Mutiny.Session current = SessionOperations.getCurrentSession();
        return current != null ? current.contains(entity) : false;
    }

    public Uni<Void> flush() {
        return getSession().chain(Session::flush);
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
        return getSession().chain(session -> session.find(entityClass, id));
    }

    public Uni<?> findById(Class<?> entityClass, Object id, LockModeType lockModeType) {
        return getSession()
                .chain(session -> session.find(entityClass, id, LockModeConverter.convertToLockMode(lockModeType)));
    }

    public PanacheQueryType find(Class<?> entityClass, String query, Object... params) {
        return find(entityClass, query, null, params);
    }

    public PanacheQueryType find(Class<?> entityClass, String query, Sort sort, Object... params) {
        String findQuery = PanacheJpaUtil.createFindQuery(entityClass, query, paramCount(params));
        Uni<Mutiny.Session> session = getSession();
        if (PanacheJpaUtil.isNamedQuery(query)) {
            String namedQuery = query.substring(1);
            if (sort != null) {
                throw new IllegalArgumentException(
                        "Sort cannot be used with named query, add an \"order by\" clause to the named query \"" + namedQuery
                                + "\" instead");
            }
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
        Uni<Mutiny.Session> session = getSession();
        if (PanacheJpaUtil.isNamedQuery(query)) {
            String namedQuery = query.substring(1);
            if (sort != null) {
                throw new IllegalArgumentException(
                        "Sort cannot be used with named query, add an \"order by\" clause to the named query \"" + namedQuery
                                + "\" instead");
            }
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

    public PanacheQueryType findAll(Class<?> entityClass) {
        String query = "FROM " + PanacheJpaUtil.getEntityName(entityClass);
        Uni<Mutiny.Session> session = getSession();
        return createPanacheQuery(session, query, null, null);
    }

    public PanacheQueryType findAll(Class<?> entityClass, Sort sort) {
        String query = "FROM " + PanacheJpaUtil.getEntityName(entityClass);
        Uni<Mutiny.Session> session = getSession();
        return createPanacheQuery(session, query, PanacheJpaUtil.toOrderBy(sort), null);
    }

    public Uni<List<?>> listAll(Class<?> entityClass) {
        return list(findAll(entityClass));
    }

    public Uni<List<?>> listAll(Class<?> entityClass, Sort sort) {
        return list(findAll(entityClass, sort));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Uni<Long> count(Class<?> entityClass) {
        return (Uni) getSession()
                .chain(session -> session.createQuery("SELECT COUNT(*) FROM " + PanacheJpaUtil.getEntityName(entityClass))
                        .getSingleResult());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Uni<Long> count(Class<?> entityClass, String query, Object... params) {

        if (PanacheJpaUtil.isNamedQuery(query))
            return (Uni) getSession().chain(session -> {
                String namedQueryName = query.substring(1);
                NamedQueryUtil.checkNamedQuery(entityClass, namedQueryName);
                return bindParameters(session.createNamedQuery(namedQueryName, Long.class), params).getSingleResult();
            });

        return (Uni) getSession().chain(session -> bindParameters(
                session.createQuery(PanacheJpaUtil.createCountQuery(entityClass, query, paramCount(params))),
                params).getSingleResult());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Uni<Long> count(Class<?> entityClass, String query, Map<String, Object> params) {

        if (PanacheJpaUtil.isNamedQuery(query))
            return (Uni) getSession().chain(session -> {
                String namedQueryName = query.substring(1);
                NamedQueryUtil.checkNamedQuery(entityClass, namedQueryName);
                return bindParameters(session.createNamedQuery(namedQueryName, Long.class), params).getSingleResult();
            });

        return (Uni) getSession().chain(session -> bindParameters(
                session.createQuery(PanacheJpaUtil.createCountQuery(entityClass, query, paramCount(params))),
                params).getSingleResult());
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
        return getSession().chain(
                session -> session.createQuery("DELETE FROM " + PanacheJpaUtil.getEntityName(entityClass)).executeUpdate()
                        .map(Integer::longValue));
    }

    public Uni<Boolean> deleteById(Class<?> entityClass, Object id) {
        // Impl note : we load the entity then delete it because it's the only implementation generic enough for any model,
        // and correct in all cases (composite key, graph of entities, ...). HQL cannot be directly used for these reasons.
        return findById(entityClass, id)
                .chain(entity -> {
                    if (entity == null) {
                        return Uni.createFrom().item(false);
                    }
                    return getSession().chain(session -> session.remove(entity).map(v -> true));
                });
    }

    public Uni<Long> delete(Class<?> entityClass, String query, Object... params) {

        if (PanacheJpaUtil.isNamedQuery(query))
            return (Uni) getSession().chain(session -> {
                String namedQueryName = query.substring(1);
                NamedQueryUtil.checkNamedQuery(entityClass, namedQueryName);
                return bindParameters(session.createNamedQuery(namedQueryName), params).executeUpdate().map(Integer::longValue);
            });

        return getSession().chain(session -> bindParameters(
                session.createQuery(PanacheJpaUtil.createDeleteQuery(entityClass, query, paramCount(params))), params)
                .executeUpdate().map(Integer::longValue));
    }

    public Uni<Long> delete(Class<?> entityClass, String query, Map<String, Object> params) {

        if (PanacheJpaUtil.isNamedQuery(query))
            return (Uni) getSession().chain(session -> {
                String namedQueryName = query.substring(1);
                NamedQueryUtil.checkNamedQuery(entityClass, namedQueryName);
                return bindParameters(session.createNamedQuery(namedQueryName), params).executeUpdate().map(Integer::longValue);
            });

        return getSession().chain(session -> bindParameters(
                session.createQuery(PanacheJpaUtil.createDeleteQuery(entityClass, query, paramCount(params))), params)
                .executeUpdate().map(Integer::longValue));
    }

    public Uni<Long> delete(Class<?> entityClass, String query, Parameters params) {
        return delete(entityClass, query, params.map());
    }

    public IllegalStateException implementationInjectionMissing() {
        return new IllegalStateException(
                "This method is normally automatically overridden in subclasses: did you forget to annotate your entity with @Entity?");
    }

    public Uni<Integer> executeUpdate(Class<?> entityClass, String query, Object... params) {

        if (PanacheJpaUtil.isNamedQuery(query))
            return (Uni) getSession().chain(session -> {
                String namedQueryName = query.substring(1);
                NamedQueryUtil.checkNamedQuery(entityClass, namedQueryName);
                return bindParameters(session.createNamedQuery(namedQueryName), params).executeUpdate();
            });

        String updateQuery = PanacheJpaUtil.createUpdateQuery(entityClass, query, paramCount(params));
        return executeUpdate(updateQuery, params);
    }

    public Uni<Integer> executeUpdate(Class<?> entityClass, String query, Map<String, Object> params) {

        if (PanacheJpaUtil.isNamedQuery(query))
            return (Uni) getSession().chain(session -> {
                String namedQueryName = query.substring(1);
                NamedQueryUtil.checkNamedQuery(entityClass, namedQueryName);
                return bindParameters(session.createNamedQuery(namedQueryName), params).executeUpdate();
            });

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

    //
    // Static helpers

    public static Uni<Mutiny.Session> getSession() {
        return SessionOperations.getSession();
    }

    public static Mutiny.Query<?> bindParameters(Mutiny.Query<?> query, Object[] params) {
        if (params == null || params.length == 0)
            return query;
        for (int i = 0; i < params.length; i++) {
            query.setParameter(i + 1, params[i]);
        }
        return query;
    }

    public static Mutiny.SelectionQuery<?> bindParameters(Mutiny.SelectionQuery<?> query, Object[] params) {
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

    public static Mutiny.SelectionQuery<?> bindParameters(Mutiny.SelectionQuery<?> query, Map<String, Object> params) {
        if (params == null || params.size() == 0)
            return query;
        for (Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        return query;
    }

    public static Uni<Integer> executeUpdate(String query, Object... params) {
        return getSession().chain(session -> {
            Mutiny.Query<?> jpaQuery = session.createQuery(query);
            bindParameters(jpaQuery, params);
            return jpaQuery.executeUpdate();
        });
    }

    public static Uni<Integer> executeUpdate(String query, Map<String, Object> params) {
        return getSession().chain(session -> {
            Mutiny.Query<?> jpaQuery = session.createQuery(query);
            bindParameters(jpaQuery, params);
            return jpaQuery.executeUpdate();
        });
    }
}
