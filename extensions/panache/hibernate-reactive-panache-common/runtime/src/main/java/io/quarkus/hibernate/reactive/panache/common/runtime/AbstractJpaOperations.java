package io.quarkus.hibernate.reactive.panache.common.runtime;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jakarta.persistence.LockModeType;

import org.hibernate.LockMode;
import org.hibernate.internal.util.LockModeConverter;
import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.hibernate.common.runtime.PanacheJpaUtil;
import io.smallrye.mutiny.Uni;

public abstract class AbstractJpaOperations<PanacheQueryType, SessionType> {

    // FIXME: make it configurable?
    static final long TIMEOUT_MS = 5000;
    protected static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    protected abstract PanacheQueryType createPanacheQuery(Uni<SessionType> session, String query, String originalQuery,
            String orderBy,
            Object paramsArrayOrMap);

    protected abstract Uni<List<?>> list(PanacheQueryType query);

    protected abstract Uni<Void> delete(SessionType session, Object entity);

    //
    // Instance methods

    private Class<SessionType> sessionType;

    protected AbstractJpaOperations(Class<SessionType> sessionType) {
        this.sessionType = sessionType;
    }

    public int paramCount(Object[] params) {
        return params != null ? params.length : 0;
    }

    public int paramCount(Map<String, Object> params) {
        return params != null ? params.size() : 0;
    }

    // These should go into a shared interface between Mutiny.Session and Mutiny.StatelessSession

    protected abstract <T> Uni<T> find(SessionType session, Class<T> entityClass, Object id);

    protected abstract <T> Uni<T> find(SessionType session, Class<T> entityClass, Object id, LockMode lockMode);

    protected abstract <R> Mutiny.SelectionQuery<R> createSelectionQuery(SessionType session, String var1, Class<R> var2);

    protected abstract <R> Mutiny.SelectionQuery<R> createNamedQuery(SessionType session, String var1, Class<R> var2);

    protected abstract <R> Mutiny.Query<R> createNamedQuery(SessionType session, String var1);

    protected abstract Mutiny.MutationQuery createMutationQuery(SessionType session, String var1);

    //
    // Queries

    public Uni<?> findById(Class<?> entityClass, Object id) {
        return getSession().chain(session -> find(session, entityClass, id));
    }

    public Uni<?> findById(Class<?> entityClass, Object id, LockModeType lockModeType) {
        return getSession()
                .chain(session -> find(session, entityClass, id, LockModeConverter.convertToLockMode(lockModeType)));
    }

    public PanacheQueryType find(Class<?> entityClass, String panacheQuery, Object... params) {
        return find(entityClass, panacheQuery, null, params);
    }

    public PanacheQueryType find(Class<?> entityClass, String panacheQuery, Sort sort, Object... params) {
        Uni<SessionType> session = getSession();
        if (PanacheJpaUtil.isNamedQuery(panacheQuery)) {
            String namedQuery = panacheQuery.substring(1);
            if (sort != null) {
                throw new IllegalArgumentException(
                        "Sort cannot be used with named query, add an \"order by\" clause to the named query \"" + namedQuery
                                + "\" instead");
            }
            NamedQueryUtil.checkNamedQuery(entityClass, namedQuery);
            return createPanacheQuery(session, panacheQuery, panacheQuery, PanacheJpaUtil.toOrderBy(sort), params);
        }
        String hqlQuery = PanacheJpaUtil.createFindQuery(entityClass, panacheQuery, paramCount(params));
        return createPanacheQuery(session, hqlQuery, panacheQuery, PanacheJpaUtil.toOrderBy(sort), params);
    }

    public PanacheQueryType find(Class<?> entityClass, String panacheQuery, Map<String, Object> params) {
        return find(entityClass, panacheQuery, null, params);
    }

    public PanacheQueryType find(Class<?> entityClass, String panacheQuery, Sort sort, Map<String, Object> params) {
        Uni<SessionType> session = getSession();
        if (PanacheJpaUtil.isNamedQuery(panacheQuery)) {
            String namedQuery = panacheQuery.substring(1);
            if (sort != null) {
                throw new IllegalArgumentException(
                        "Sort cannot be used with named query, add an \"order by\" clause to the named query \"" + namedQuery
                                + "\" instead");
            }
            NamedQueryUtil.checkNamedQuery(entityClass, namedQuery);
            return createPanacheQuery(session, panacheQuery, panacheQuery, PanacheJpaUtil.toOrderBy(sort), params);
        }
        String hqlQuery = PanacheJpaUtil.createFindQuery(entityClass, panacheQuery, paramCount(params));
        return createPanacheQuery(session, hqlQuery, panacheQuery, PanacheJpaUtil.toOrderBy(sort), params);
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
        Uni<SessionType> session = getSession();
        return createPanacheQuery(session, query, null, null, null);
    }

    public PanacheQueryType findAll(Class<?> entityClass, Sort sort) {
        String query = "FROM " + PanacheJpaUtil.getEntityName(entityClass);
        Uni<SessionType> session = getSession();
        return createPanacheQuery(session, query, null, PanacheJpaUtil.toOrderBy(sort), null);
    }

    public Uni<List<?>> listAll(Class<?> entityClass) {
        return list(findAll(entityClass));
    }

    public Uni<List<?>> listAll(Class<?> entityClass, Sort sort) {
        return list(findAll(entityClass, sort));
    }

    public Uni<Long> count(Class<?> entityClass) {
        return getSession()
                .chain(session -> createSelectionQuery(session, "FROM " + PanacheJpaUtil.getEntityName(entityClass),
                        entityClass)
                        .getResultCount());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Uni<Long> count(Class<?> entityClass, String panacheQuery, Object... params) {

        if (PanacheJpaUtil.isNamedQuery(panacheQuery))
            return (Uni) getSession().chain(session -> {
                String namedQueryName = panacheQuery.substring(1);
                NamedQueryUtil.checkNamedQuery(entityClass, namedQueryName);
                return bindParameters(createNamedQuery(session, namedQueryName, Long.class), params).getSingleResult();
            });

        return getSession().chain(session -> bindParameters(
                createSelectionQuery(session, PanacheJpaUtil.createQueryForCount(entityClass, panacheQuery, paramCount(params)),
                        Object.class),
                params).getResultCount())
                .onFailure(RuntimeException.class)
                .transform(x -> NamedQueryUtil.checkForNamedQueryMistake((RuntimeException) x, panacheQuery));
    }

    public Uni<Long> count(Class<?> entityClass, String panacheQuery, Map<String, Object> params) {

        if (PanacheJpaUtil.isNamedQuery(panacheQuery))
            return getSession().chain(session -> {
                String namedQueryName = panacheQuery.substring(1);
                NamedQueryUtil.checkNamedQuery(entityClass, namedQueryName);
                return bindParameters(createNamedQuery(session, namedQueryName, Long.class), params).getSingleResult();
            });

        return getSession().chain(session -> bindParameters(
                createSelectionQuery(session, PanacheJpaUtil.createQueryForCount(entityClass, panacheQuery, paramCount(params)),
                        Object.class),
                params).getResultCount())
                .onFailure(RuntimeException.class)
                .transform(x -> NamedQueryUtil.checkForNamedQueryMistake((RuntimeException) x, panacheQuery));
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
                session -> createMutationQuery(session, "DELETE FROM " + PanacheJpaUtil.getEntityName(entityClass))
                        .executeUpdate()
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
                    return getSession().chain(session -> delete(session, entity).map(v -> true));
                });
    }

    public Uni<Long> delete(Class<?> entityClass, String panacheQuery, Object... params) {

        if (PanacheJpaUtil.isNamedQuery(panacheQuery))
            return getSession().chain(session -> {
                String namedQueryName = panacheQuery.substring(1);
                NamedQueryUtil.checkNamedQuery(entityClass, namedQueryName);
                return bindParameters(createNamedQuery(session, namedQueryName), params).executeUpdate()
                        .map(Integer::longValue);
            });

        return getSession().chain(session -> bindParameters(
                createMutationQuery(session, PanacheJpaUtil.createDeleteQuery(entityClass, panacheQuery, paramCount(params))),
                params)
                .executeUpdate().map(Integer::longValue))
                .onFailure(RuntimeException.class)
                .transform(x -> NamedQueryUtil.checkForNamedQueryMistake((RuntimeException) x, panacheQuery));
    }

    public Uni<Long> delete(Class<?> entityClass, String panacheQuery, Map<String, Object> params) {

        if (PanacheJpaUtil.isNamedQuery(panacheQuery))
            return getSession().chain(session -> {
                String namedQueryName = panacheQuery.substring(1);
                NamedQueryUtil.checkNamedQuery(entityClass, namedQueryName);
                return bindParameters(createNamedQuery(session, namedQueryName), params).executeUpdate()
                        .map(Integer::longValue);
            });

        return getSession().chain(session -> bindParameters(
                createMutationQuery(session, PanacheJpaUtil.createDeleteQuery(entityClass, panacheQuery, paramCount(params))),
                params)
                .executeUpdate().map(Integer::longValue))
                .onFailure(RuntimeException.class)
                .transform(x -> NamedQueryUtil.checkForNamedQueryMistake((RuntimeException) x, panacheQuery));
    }

    public Uni<Long> delete(Class<?> entityClass, String query, Parameters params) {
        return delete(entityClass, query, params.map());
    }

    public IllegalStateException implementationInjectionMissing() {
        return new IllegalStateException(
                "This method is normally automatically overridden in subclasses: did you forget to annotate your entity with @Entity?");
    }

    public Uni<Integer> executeUpdate(Class<?> entityClass, String panacheQuery, Object... params) {

        if (PanacheJpaUtil.isNamedQuery(panacheQuery))
            return (Uni) getSession().chain(session -> {
                String namedQueryName = panacheQuery.substring(1);
                NamedQueryUtil.checkNamedQuery(entityClass, namedQueryName);
                return bindParameters(createNamedQuery(session, namedQueryName), params).executeUpdate();
            });

        String updateQuery = PanacheJpaUtil.createUpdateQuery(entityClass, panacheQuery, paramCount(params));
        return executeUpdate(updateQuery, params)
                .onFailure(RuntimeException.class)
                .transform(x -> NamedQueryUtil.checkForNamedQueryMistake((RuntimeException) x, panacheQuery));
    }

    public Uni<Integer> executeUpdate(Class<?> entityClass, String panacheQuery, Map<String, Object> params) {

        if (PanacheJpaUtil.isNamedQuery(panacheQuery))
            return (Uni) getSession().chain(session -> {
                String namedQueryName = panacheQuery.substring(1);
                NamedQueryUtil.checkNamedQuery(entityClass, namedQueryName);
                return bindParameters(createNamedQuery(session, namedQueryName), params).executeUpdate();
            });

        String updateQuery = PanacheJpaUtil.createUpdateQuery(entityClass, panacheQuery, paramCount(params));
        return executeUpdate(updateQuery, params)
                .onFailure(RuntimeException.class)
                .transform(x -> NamedQueryUtil.checkForNamedQueryMistake((RuntimeException) x, panacheQuery));
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

    public Uni<SessionType> getSession() {
        return sessionType == Mutiny.Session.class ? (Uni<SessionType>) SessionOperations.getSession()
                : (Uni<SessionType>) SessionOperations.getStatelessSession();
    }

    //
    // Static helpers

    public static Mutiny.Query<?> bindParameters(Mutiny.Query<?> query, Object[] params) {
        if (params == null || params.length == 0)
            return query;
        for (int i = 0; i < params.length; i++) {
            query.setParameter(i + 1, params[i]);
        }
        return query;
    }

    public static <T extends Mutiny.AbstractQuery> T bindParameters(T query, Object[] params) {
        if (params == null || params.length == 0)
            return query;
        for (int i = 0; i < params.length; i++) {
            query.setParameter(i + 1, params[i]);
        }
        return query;
    }

    public static <T extends Mutiny.AbstractQuery> T bindParameters(T query, Map<String, Object> params) {
        if (params == null || params.size() == 0)
            return query;
        for (Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        return query;
    }

    public Uni<Integer> executeUpdate(String query, Object... params) {
        return getSession().chain(session -> bindParameters(createMutationQuery(session, query), params)
                .executeUpdate());
    }

    public Uni<Integer> executeUpdate(String query, Map<String, Object> params) {
        return getSession().chain(session -> bindParameters(createMutationQuery(session, query), params)
                .executeUpdate());
    }
}
