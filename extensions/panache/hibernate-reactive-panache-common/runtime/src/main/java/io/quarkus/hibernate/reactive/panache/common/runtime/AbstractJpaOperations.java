package io.quarkus.hibernate.reactive.panache.common.runtime;

import static io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
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
    private static final Map<String, String> entityToPersistenceUnit = new HashMap<>();

    // Putting synchronized here because fields involved were marked as volatile initially,
    // so I expect recorders can be called concurrently?
    public static void addEntityTypesToPersistenceUnit(Map<String, String> map) {
        // Note: this may be called multiple times if an app uses both Java and Kotlin.
        // We don't really test what happens if entities are defined both in Java and Kotlin at the moment,
        // so we mostly care about the case where this gets called once with an empty map, and once with a non-empty map:
        // in that case, we don't want the empty map to erase the other one.
        entityToPersistenceUnit.putAll(map);
    }

    // FIXME: make it configurable?
    static final long TIMEOUT_MS = 5000;
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    protected abstract PanacheQueryType createPanacheQuery(Uni<Mutiny.Session> session, String query, String originalQuery,
            String orderBy,
            Object paramsArrayOrMap);

    protected abstract Uni<List<?>> list(PanacheQueryType query);

    //
    // Instance methods

    public Uni<Void> persist(Object entity) {
        return persist(getSession(entity.getClass()), entity);
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
        Map<String, List<Object>> sessions = Arrays.stream(entities)
                .collect(Collectors.groupingBy(e -> entityToPersistenceUnit.get(e.getClass().getName())));

        List<Uni<Void>> results = new ArrayList<>();
        for (Entry<String, List<Object>> entry : sessions.entrySet()) {
            results.add(getSession(entry.getKey()).chain(session -> session.persistAll(entry.getValue().toArray())));
        }

        return Uni.combine().all().unis(results).discardItems();
    }

    public Uni<Void> delete(Object entity) {
        return getSession(entity.getClass()).chain(session -> session.remove(entity));
    }

    public boolean isPersistent(Object entity) {
        Session currentSession = getCurrentSession(entity.getClass());
        if (currentSession == null) {
            // No active session so object is surely non-persistent
            return false;
        }

        return currentSession.contains(entity);
    }

    public Session getCurrentSession(Class<?> entityClass) {
        String persistenceUnitName = entityToPersistenceUnit.get(entityClass.getName());
        return SessionOperations.getCurrentSession(persistenceUnitName);
    }

    public Uni<Void> flush(Object entity) {
        return getSession(entity.getClass()).chain(Session::flush);
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
        return getSession(entityClass).chain(session -> session.find(entityClass, id));
    }

    public Uni<?> findById(Class<?> entityClass, Object id, LockModeType lockModeType) {
        return getSession(entityClass)
                .chain(session -> session.find(entityClass, id, LockModeConverter.convertToLockMode(lockModeType)));
    }

    public PanacheQueryType find(Class<?> entityClass, String panacheQuery, Object... params) {
        return find(entityClass, panacheQuery, null, params);
    }

    public PanacheQueryType find(Class<?> entityClass, String panacheQuery, Sort sort, Object... params) {
        Uni<Mutiny.Session> session = getSession(entityClass);
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
        Uni<Mutiny.Session> session = getSession(entityClass);
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
        Uni<Mutiny.Session> session = getSession(entityClass);
        return createPanacheQuery(session, query, null, null, null);
    }

    public PanacheQueryType findAll(Class<?> entityClass, Sort sort) {
        String query = "FROM " + PanacheJpaUtil.getEntityName(entityClass);
        Uni<Mutiny.Session> session = getSession(entityClass);
        return createPanacheQuery(session, query, null, PanacheJpaUtil.toOrderBy(sort), null);
    }

    public Uni<List<?>> listAll(Class<?> entityClass) {
        return list(findAll(entityClass));
    }

    public Uni<List<?>> listAll(Class<?> entityClass, Sort sort) {
        return list(findAll(entityClass, sort));
    }

    public Uni<Long> count(Class<?> entityClass) {
        return getSession(entityClass)
                .chain(session -> session
                        .createSelectionQuery("FROM " + PanacheJpaUtil.getEntityName(entityClass), entityClass)
                        .getResultCount());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Uni<Long> count(Class<?> entityClass, String panacheQuery, Object... params) {

        if (PanacheJpaUtil.isNamedQuery(panacheQuery))
            return (Uni) getSession(entityClass).chain(session -> {
                String namedQueryName = panacheQuery.substring(1);
                NamedQueryUtil.checkNamedQuery(entityClass, namedQueryName);
                return bindParameters(session.createNamedQuery(namedQueryName, Long.class), params).getSingleResult();
            });

        return getSession(entityClass).chain(session -> bindParameters(
                session.createSelectionQuery(PanacheJpaUtil.createQueryForCount(entityClass, panacheQuery, paramCount(params)),
                        Object.class),
                params).getResultCount())
                .onFailure(RuntimeException.class)
                .transform(x -> NamedQueryUtil.checkForNamedQueryMistake((RuntimeException) x, panacheQuery));
    }

    public Uni<Long> count(Class<?> entityClass, String panacheQuery, Map<String, Object> params) {

        if (PanacheJpaUtil.isNamedQuery(panacheQuery))
            return getSession(entityClass).chain(session -> {
                String namedQueryName = panacheQuery.substring(1);
                NamedQueryUtil.checkNamedQuery(entityClass, namedQueryName);
                return bindParameters(session.createNamedQuery(namedQueryName, Long.class), params).getSingleResult();
            });

        return getSession(entityClass).chain(session -> bindParameters(
                session.createSelectionQuery(PanacheJpaUtil.createQueryForCount(entityClass, panacheQuery, paramCount(params)),
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
        return getSession(entityClass).chain(
                session -> session.createMutationQuery("DELETE FROM " + PanacheJpaUtil.getEntityName(entityClass))
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
                    return getSession(entityClass).chain(session -> session.remove(entity).map(v -> true));
                });
    }

    public Uni<Long> delete(Class<?> entityClass, String panacheQuery, Object... params) {

        if (PanacheJpaUtil.isNamedQuery(panacheQuery))
            return getSession(entityClass).chain(session -> {
                String namedQueryName = panacheQuery.substring(1);
                NamedQueryUtil.checkNamedQuery(entityClass, namedQueryName);
                return bindParameters(session.createNamedQuery(namedQueryName), params).executeUpdate().map(Integer::longValue);
            });

        return getSession(entityClass).chain(session -> bindParameters(
                session.createMutationQuery(PanacheJpaUtil.createDeleteQuery(entityClass, panacheQuery, paramCount(params))),
                params)
                .executeUpdate().map(Integer::longValue))
                .onFailure(RuntimeException.class)
                .transform(x -> NamedQueryUtil.checkForNamedQueryMistake((RuntimeException) x, panacheQuery));
    }

    public Uni<Long> delete(Class<?> entityClass, String panacheQuery, Map<String, Object> params) {

        if (PanacheJpaUtil.isNamedQuery(panacheQuery))
            return getSession(entityClass).chain(session -> {
                String namedQueryName = panacheQuery.substring(1);
                NamedQueryUtil.checkNamedQuery(entityClass, namedQueryName);
                return bindParameters(session.createNamedQuery(namedQueryName), params).executeUpdate().map(Integer::longValue);
            });

        return getSession(entityClass).chain(session -> bindParameters(
                session.createMutationQuery(PanacheJpaUtil.createDeleteQuery(entityClass, panacheQuery, paramCount(params))),
                params)
                .executeUpdate().map(Integer::longValue))
                .onFailure(RuntimeException.class)
                .transform(x -> NamedQueryUtil.checkForNamedQueryMistake((RuntimeException) x, panacheQuery));
    }

    public Uni<Long> delete(Class<?> entityClass, String query, Parameters params) {
        return delete(entityClass, query, params.map());
    }

    public static IllegalStateException implementationInjectionMissing() {
        return new IllegalStateException(
                "This method is normally automatically overridden in subclasses: did you forget to annotate your entity with @Entity?");
    }

    public Uni<Integer> executeUpdate(Class<?> entityClass, String panacheQuery, Object... params) {

        if (PanacheJpaUtil.isNamedQuery(panacheQuery))
            return (Uni) getSession(entityClass).chain(session -> {
                String namedQueryName = panacheQuery.substring(1);
                NamedQueryUtil.checkNamedQuery(entityClass, namedQueryName);
                return bindParameters(session.createNamedQuery(namedQueryName), params).executeUpdate();
            });

        String updateQuery = PanacheJpaUtil.createUpdateQuery(entityClass, panacheQuery, paramCount(params));
        return executeUpdate(updateQuery, params)
                .onFailure(RuntimeException.class)
                .transform(x -> NamedQueryUtil.checkForNamedQueryMistake((RuntimeException) x, panacheQuery));
    }

    public Uni<Integer> executeUpdate(Class<?> entityClass, String panacheQuery, Map<String, Object> params) {

        if (PanacheJpaUtil.isNamedQuery(panacheQuery))
            return (Uni) getSession(entityClass).chain(session -> {
                String namedQueryName = panacheQuery.substring(1);
                NamedQueryUtil.checkNamedQuery(entityClass, namedQueryName);
                return bindParameters(session.createNamedQuery(namedQueryName), params).executeUpdate();
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

    //
    // Static helpers

    public Uni<Mutiny.Session> getSession() {
        return getSession(DEFAULT_PERSISTENCE_UNIT_NAME);
    }

    public Uni<Mutiny.Session> getSession(Class<?> clazz) {
        String className = clazz.getName();
        String persistenceUnitName = entityToPersistenceUnit.get(className);
        return getSession(persistenceUnitName);
    }

    public Uni<Mutiny.Session> getSession(String persistenceUnitName) {
        return SessionOperations.getSession(persistenceUnitName);
    }

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

    /**
     * Execute update on default persistence unit
     */
    public Uni<Integer> executeUpdate(String query, Object... params) {
        return getSession(DEFAULT_PERSISTENCE_UNIT_NAME)
                .chain(session -> bindParameters(session.createMutationQuery(query), params)
                        .executeUpdate());
    }

    /**
     * Execute update on default persistence unit
     */
    public Uni<Integer> executeUpdate(String query, Map<String, Object> params) {
        return getSession(DEFAULT_PERSISTENCE_UNIT_NAME)
                .chain(session -> bindParameters(session.createMutationQuery(query), params)
                        .executeUpdate());
    }
}
