package io.quarkus.hibernate.orm.panache.common.runtime;

import static io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Stream;

import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;

import org.hibernate.Session;
import org.hibernate.SharedSessionContract;
import org.hibernate.query.CommonQueryContract;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.SelectionQuery;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import io.quarkus.panache.hibernate.common.runtime.PanacheJpaUtil;

public abstract class AbstractJpaOperations<PanacheQueryType, SessionType extends SharedSessionContract> {

    //
    // Static

    private static final Map<String, String> entityToPersistenceUnit = new HashMap<>();
    private static volatile Boolean entityToPersistenceUnitIsIncomplete = null;

    // Putting synchronized here because fields involved were marked as volatile initially,
    // so I expect recorders can be called concurrently?
    public static synchronized void addEntityTypesToPersistenceUnit(Map<String, String> map, boolean incomplete) {
        // Note: this may be called multiple times if an app uses both Java and Kotlin.
        // We don't really test what happens if entities are defined both in Java and Kotlin at the moment,
        // so we mostly care about the case where this gets called once with an empty map, and once with a non-empty map:
        // in that case, we don't want the empty map to erase the other one.
        entityToPersistenceUnit.putAll(map);
        if (entityToPersistenceUnitIsIncomplete == null) {
            entityToPersistenceUnitIsIncomplete = incomplete;
        } else {
            entityToPersistenceUnitIsIncomplete = entityToPersistenceUnitIsIncomplete || incomplete;
        }
    }

    private static volatile Map<Class<?>, Class<?>> repositoryClassToEntityClass = Collections.emptyMap();

    public static void setRepositoryClassesToEntityClasses(Map<Class<?>, Class<?>> map) {
        repositoryClassToEntityClass = Collections.unmodifiableMap(map);
    }

    public static <Entity> Class<? extends Entity> getRepositoryEntityClass(
            // FIXME: if we move this to JpaOperations we can add a type constraint on the repo class
            Class<?> repositoryImplementationClass) {
        Class<?> ret = repositoryClassToEntityClass.get(repositoryImplementationClass);
        if (ret == null) {
            throw new RuntimeException("Your repository class " + repositoryImplementationClass
                    + " was not properly detected and assigned an entity type");
        }
        return (Class<? extends Entity>) ret;
    }

    //
    // Instance

    private Class<SessionType> sessionType;

    protected AbstractJpaOperations(Class<SessionType> sessionType) {
        this.sessionType = sessionType;
    }

    protected abstract PanacheQueryType createPanacheQuery(SessionType session, String query, String originalQuery,
            String orderBy,
            Object paramsArrayOrMap);

    public abstract List<?> list(PanacheQueryType query);

    public abstract Stream<?> stream(PanacheQueryType query);

    /**
     * Returns the {@link Session} for the given {@link Class<?> entity}
     *
     * @return {@link Session}
     */
    public SessionType getSession(Class<?> clazz) {
        String clazzName = clazz.getName();
        String persistentUnitName = entityToPersistenceUnit.get(clazzName);
        if (persistentUnitName == null) {
            if (entityToPersistenceUnitIsIncomplete == null || entityToPersistenceUnitIsIncomplete) {
                // When using persistence.xml, `entityToPersistenceUnit` is most likely empty,
                // so we'll just return the default PU and hope for the best.
                // The error will be thrown later by Hibernate ORM if necessary;
                // it will be a bit less clear, but this is an edge case.
                SessionType session = getSession(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME);
                if (session != null) {
                    return session;
                }
            }
            // For Quarkus-configured PUs, or if there is no PU, this is definitely an error.
            throw new IllegalStateException(String.format(
                    "Entity '%s' was not found. Did you forget to annotate your Panache Entity classes with '@Entity'?",
                    clazzName));
        }
        return getSession(persistentUnitName);
    }

    public SessionType getSession(String persistentUnitName) {
        ArcContainer arcContainer = Arc.container();
        if (persistentUnitName == null || PersistenceUnitUtil.isDefaultPersistenceUnit(persistentUnitName)) {
            return arcContainer.instance(sessionType).get();
        } else {
            return arcContainer.instance(sessionType,
                    new PersistenceUnit.PersistenceUnitLiteral(persistentUnitName))
                    .get();
        }
    }

    public SessionType getSession() {
        return getSession(DEFAULT_PERSISTENCE_UNIT_NAME);
    }
    //
    // Instance methods

    //
    // Private stuff

    public static TransactionManager getTransactionManager() {
        return Arc.container().instance(TransactionManager.class).get();
    }

    public static <T extends CommonQueryContract> T bindParameters(T query, Object[] params) {
        if (params == null || params.length == 0)
            return query;
        for (int i = 0; i < params.length; i++) {
            query.setParameter(i + 1, params[i]);
        }
        return query;
    }

    public static <T extends CommonQueryContract> T bindParameters(T query, Map<String, Object> params) {
        if (params == null || params.isEmpty())
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

    public PanacheQueryType find(Class<?> entityClass, String query, Object... params) {
        return find(entityClass, query, null, params);
    }

    public PanacheQueryType find(Class<?> entityClass, String panacheQuery, Sort sort, Object... params) {
        SessionType session = getSession(entityClass);
        if (PanacheJpaUtil.isNamedQuery(panacheQuery)) {
            String namedQuery = panacheQuery.substring(1);
            if (sort != null) {
                throw new IllegalArgumentException(
                        "Sort cannot be used with named query, add an \"order by\" clause to the named query \"" + namedQuery
                                + "\" instead");
            }
            NamedQueryUtil.checkNamedQuery(entityClass, namedQuery);
            return createPanacheQuery(session, panacheQuery, panacheQuery, null, params);
        }

        String translatedHqlQuery = PanacheJpaUtil.createFindQuery(entityClass, panacheQuery, paramCount(params));
        return createPanacheQuery(session, translatedHqlQuery, panacheQuery, PanacheJpaUtil.toOrderBy(sort), params);
    }

    public PanacheQueryType find(Class<?> entityClass, String panacheQuery, Map<String, Object> params) {
        return find(entityClass, panacheQuery, null, params);
    }

    public PanacheQueryType find(Class<?> entityClass, String panacheQuery, Sort sort, Map<String, Object> params) {
        SessionType session = getSession(entityClass);
        if (PanacheJpaUtil.isNamedQuery(panacheQuery)) {
            String namedQuery = panacheQuery.substring(1);
            if (sort != null) {
                throw new IllegalArgumentException(
                        "Sort cannot be used with named query, add an \"order by\" clause to the named query \"" + namedQuery
                                + "\" instead");
            }
            NamedQueryUtil.checkNamedQuery(entityClass, namedQuery);
            return createPanacheQuery(session, panacheQuery, panacheQuery, null, params);
        }

        String translatedHqlQuery = PanacheJpaUtil.createFindQuery(entityClass, panacheQuery, paramCount(params));
        return createPanacheQuery(session, translatedHqlQuery, panacheQuery, PanacheJpaUtil.toOrderBy(sort), params);
    }

    public PanacheQueryType find(Class<?> entityClass, String panacheQuery, Parameters params) {
        return find(entityClass, panacheQuery, null, params);
    }

    public PanacheQueryType find(Class<?> entityClass, String panacheQuery, Sort sort, Parameters params) {
        return find(entityClass, panacheQuery, sort, params.map());
    }

    public List<?> list(Class<?> entityClass, String panacheQuery, Object... params) {
        return list(find(entityClass, panacheQuery, params));
    }

    public List<?> list(Class<?> entityClass, String panacheQuery, Sort sort, Object... params) {
        return list(find(entityClass, panacheQuery, sort, params));
    }

    public List<?> list(Class<?> entityClass, String panacheQuery, Map<String, Object> params) {
        return list(find(entityClass, panacheQuery, params));
    }

    public List<?> list(Class<?> entityClass, String panacheQuery, Sort sort, Map<String, Object> params) {
        return list(find(entityClass, panacheQuery, sort, params));
    }

    public List<?> list(Class<?> entityClass, String panacheQuery, Parameters params) {
        return list(find(entityClass, panacheQuery, params));
    }

    public List<?> list(Class<?> entityClass, String panacheQuery, Sort sort, Parameters params) {
        return list(find(entityClass, panacheQuery, sort, params));
    }

    public Stream<?> stream(Class<?> entityClass, String panacheQuery, Object... params) {
        return stream(find(entityClass, panacheQuery, params));
    }

    public Stream<?> stream(Class<?> entityClass, String panacheQuery, Sort sort, Object... params) {
        return stream(find(entityClass, panacheQuery, sort, params));
    }

    public Stream<?> stream(Class<?> entityClass, String panacheQuery, Map<String, Object> params) {
        return stream(find(entityClass, panacheQuery, params));
    }

    public Stream<?> stream(Class<?> entityClass, String panacheQuery, Sort sort, Map<String, Object> params) {
        return stream(find(entityClass, panacheQuery, sort, params));
    }

    public Stream<?> stream(Class<?> entityClass, String panacheQuery, Parameters params) {
        return stream(find(entityClass, panacheQuery, params));
    }

    public Stream<?> stream(Class<?> entityClass, String panacheQuery, Sort sort, Parameters params) {
        return stream(find(entityClass, panacheQuery, sort, params));
    }

    public PanacheQueryType findAll(Class<?> entityClass) {
        String query = "FROM " + PanacheJpaUtil.getEntityName(entityClass);
        SessionType session = getSession(entityClass);
        return createPanacheQuery(session, query, null, null, null);
    }

    public PanacheQueryType findAll(Class<?> entityClass, Sort sort) {
        String query = "FROM " + PanacheJpaUtil.getEntityName(entityClass);
        SessionType session = getSession(entityClass);
        return createPanacheQuery(session, query, null, PanacheJpaUtil.toOrderBy(sort), null);
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
        return getSession(entityClass)
                .createSelectionQuery("FROM " + PanacheJpaUtil.getEntityName(entityClass), entityClass)
                .getResultCount();
    }

    public long count(Class<?> entityClass, String panacheQuery, Object... params) {
        if (PanacheJpaUtil.isNamedQuery(panacheQuery)) {
            SelectionQuery<?> namedQuery = extractNamedSelectionQuery(entityClass, panacheQuery);
            return (long) bindParameters(namedQuery, params).getSingleResult();
        }

        try {
            String query = PanacheJpaUtil.createQueryForCount(entityClass, panacheQuery, paramCount(params));
            return bindParameters(getSession(entityClass).createSelectionQuery(query, Object.class), params).getResultCount();
        } catch (RuntimeException x) {
            throw NamedQueryUtil.checkForNamedQueryMistake(x, panacheQuery);
        }
    }

    public long count(Class<?> entityClass, String panacheQuery, Map<String, Object> params) {
        if (PanacheJpaUtil.isNamedQuery(panacheQuery)) {
            SelectionQuery<?> namedQuery = extractNamedSelectionQuery(entityClass, panacheQuery);
            return (long) bindParameters(namedQuery, params).getSingleResult();
        }

        try {
            String query = PanacheJpaUtil.createQueryForCount(entityClass, panacheQuery, paramCount(params));
            return bindParameters(getSession(entityClass).createSelectionQuery(query, Object.class), params).getResultCount();
        } catch (RuntimeException x) {
            throw NamedQueryUtil.checkForNamedQueryMistake(x, panacheQuery);
        }

    }

    public long count(Class<?> entityClass, String query, Parameters params) {
        return count(entityClass, query, params.map());
    }

    private SelectionQuery<?> extractNamedSelectionQuery(Class<?> entityClass, String query) {
        String namedQueryName = extractNamedQueryName(entityClass, query);
        return getSession(entityClass).createNamedSelectionQuery(namedQueryName);
    }

    private MutationQuery extractNamedMutationQuery(Class<?> entityClass, String query) {
        String namedQueryName = extractNamedQueryName(entityClass, query);
        return getSession(entityClass).createNamedMutationQuery(namedQueryName);
    }

    private String extractNamedQueryName(Class<?> entityClass, String query) {
        if (!PanacheJpaUtil.isNamedQuery(query))
            throw new IllegalArgumentException("Must be a named query!");

        String namedQueryName = query.substring(1);
        NamedQueryUtil.checkNamedQuery(entityClass, namedQueryName);
        return namedQueryName;
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
        return getSession(entityClass).createMutationQuery("DELETE FROM " + PanacheJpaUtil.getEntityName(entityClass))
                .executeUpdate();
    }

    public long delete(Class<?> entityClass, String panacheQuery, Object... params) {
        if (PanacheJpaUtil.isNamedQuery(panacheQuery)) {
            return bindParameters(extractNamedMutationQuery(entityClass, panacheQuery), params).executeUpdate();
        }

        try {
            return bindParameters(
                    getSession(entityClass).createMutationQuery(
                            PanacheJpaUtil.createDeleteQuery(entityClass, panacheQuery, paramCount(params))),
                    params)
                    .executeUpdate();
        } catch (RuntimeException x) {
            throw NamedQueryUtil.checkForNamedQueryMistake(x, panacheQuery);
        }
    }

    public long delete(Class<?> entityClass, String panacheQuery, Map<String, Object> params) {
        if (PanacheJpaUtil.isNamedQuery(panacheQuery)) {
            return bindParameters(extractNamedMutationQuery(entityClass, panacheQuery), params).executeUpdate();
        }

        try {
            return bindParameters(
                    getSession(entityClass)
                            .createMutationQuery(
                                    PanacheJpaUtil.createDeleteQuery(entityClass, panacheQuery, paramCount(params))),
                    params)
                    .executeUpdate();
        } catch (RuntimeException x) {
            throw NamedQueryUtil.checkForNamedQueryMistake(x, panacheQuery);
        }
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
        return bindParameters(getSession(DEFAULT_PERSISTENCE_UNIT_NAME).createMutationQuery(query), params)
                .executeUpdate();
    }

    /**
     * Execute update on default persistence unit
     */
    public int executeUpdate(String query, Map<String, Object> params) {
        return bindParameters(getSession(DEFAULT_PERSISTENCE_UNIT_NAME).createMutationQuery(query), params)
                .executeUpdate();
    }

    public int executeUpdate(Class<?> entityClass, String panacheQuery, Object... params) {
        if (PanacheJpaUtil.isNamedQuery(panacheQuery)) {
            return bindParameters(extractNamedMutationQuery(entityClass, panacheQuery), params).executeUpdate();
        }

        try {
            String updateQuery = PanacheJpaUtil.createUpdateQuery(entityClass, panacheQuery, paramCount(params));
            return bindParameters(getSession(entityClass).createMutationQuery(updateQuery), params)
                    .executeUpdate();
        } catch (RuntimeException x) {
            throw NamedQueryUtil.checkForNamedQueryMistake(x, panacheQuery);
        }
    }

    public int executeUpdate(Class<?> entityClass, String panacheQuery, Map<String, Object> params) {
        if (PanacheJpaUtil.isNamedQuery(panacheQuery)) {
            return bindParameters(extractNamedMutationQuery(entityClass, panacheQuery), params).executeUpdate();
        }

        try {
            String updateQuery = PanacheJpaUtil.createUpdateQuery(entityClass, panacheQuery, paramCount(params));
            return bindParameters(getSession(entityClass).createMutationQuery(updateQuery), params)
                    .executeUpdate();
        } catch (RuntimeException x) {
            throw NamedQueryUtil.checkForNamedQueryMistake(x, panacheQuery);
        }
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
