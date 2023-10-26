package io.quarkus.hibernate.orm.runtime.session;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.inject.Instance;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TransactionRequiredException;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.hibernate.CacheMode;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.IdentifierLoadAccess;
import org.hibernate.LobHelper;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.NaturalIdMultiLoadAccess;
import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.Transaction;
import org.hibernate.UnknownProfileException;
import org.hibernate.graph.RootGraph;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.MutationQuery;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaInsertSelect;
import org.hibernate.stat.SessionStatistics;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.runtime.RequestScopedSessionHolder;
import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.BlockingOperationNotAllowedException;

public class TransactionScopedSession implements Session {

    protected static final String TRANSACTION_IS_NOT_ACTIVE = "Transaction is not active, consider adding @Transactional to your method to automatically activate one.";

    private final TransactionManager transactionManager;
    private final TransactionSynchronizationRegistry transactionSynchronizationRegistry;
    private final SessionFactory sessionFactory;
    private final JTASessionOpener jtaSessionOpener;
    private final String unitName;
    private final String sessionKey;
    private final Instance<RequestScopedSessionHolder> requestScopedSessions;

    public TransactionScopedSession(TransactionManager transactionManager,
            TransactionSynchronizationRegistry transactionSynchronizationRegistry,
            SessionFactory sessionFactory,
            String unitName,
            Instance<RequestScopedSessionHolder> requestScopedSessions) {
        this.transactionManager = transactionManager;
        this.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
        this.sessionFactory = sessionFactory;
        this.jtaSessionOpener = JTASessionOpener.create(sessionFactory);
        this.unitName = unitName;
        this.sessionKey = this.getClass().getSimpleName() + "-" + unitName;
        this.requestScopedSessions = requestScopedSessions;
    }

    SessionResult acquireSession() {
        if (isInTransaction()) {
            Session session = (Session) transactionSynchronizationRegistry.getResource(sessionKey);
            if (session != null) {
                return new SessionResult(session, false, true);
            }
            Session newSession = jtaSessionOpener.openSession();
            // The session has automatically joined the JTA transaction when it was constructed.
            transactionSynchronizationRegistry.putResource(sessionKey, newSession);
            // No need to flush or close the session upon transaction completion:
            // Hibernate ORM itself registers a transaction that does just that.
            // See:
            // - io.quarkus.hibernate.orm.runtime.boot.FastBootMetadataBuilder.mergeSettings
            // - org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl.joinJtaTransaction
            // - org.hibernate.internal.SessionImpl.beforeTransactionCompletion
            // - org.hibernate.internal.SessionImpl.afterTransactionCompletion
            return new SessionResult(newSession, false, true);
        } else if (Arc.container().requestContext().isActive()) {
            RequestScopedSessionHolder requestScopedSessions = this.requestScopedSessions.get();
            return new SessionResult(requestScopedSessions.getOrCreateSession(unitName, sessionFactory),
                    false, false);
        } else {
            throw new ContextNotActiveException(
                    "Cannot use the EntityManager/Session because neither a transaction nor a CDI request context is active."
                            + " Consider adding @Transactional to your method to automatically activate a transaction,"
                            + " or @ActivateRequestContext if you have valid reasons not to use transactions.");
        }
    }

    private void checkBlocking() {
        if (!BlockingOperationControl.isBlockingAllowed()) {
            throw new BlockingOperationNotAllowedException(
                    "You have attempted to perform a blocking operation on a IO thread. This is not allowed, as blocking the IO thread will cause major performance issues with your application. If you want to perform blocking EntityManager operations make sure you are doing it from a worker thread.");
        }
    }

    private boolean isInTransaction() {
        try {
            switch (transactionManager.getStatus()) {
                case Status.STATUS_ACTIVE:
                case Status.STATUS_COMMITTING:
                case Status.STATUS_MARKED_ROLLBACK:
                case Status.STATUS_PREPARED:
                case Status.STATUS_PREPARING:
                    return true;
                default:
                    return false;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void persist(Object entity) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            emr.session.persist(entity);
        }
    }

    @Override
    public <T> T merge(T entity) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            return emr.session.merge(entity);
        }
    }

    @Override
    public void remove(Object entity) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            emr.session.remove(entity);
        }
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.find(entityClass, primaryKey);
        }
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, Map<String, Object> properties) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.find(entityClass, primaryKey, properties);
        }
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.find(entityClass, primaryKey, lockMode);
        }
    }

    @Override
    public <T> T find(Class<T> entityClass, Object primaryKey, LockModeType lockMode, Map<String, Object> properties) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.find(entityClass, primaryKey, lockMode, properties);
        }
    }

    @Override
    public <T> T getReference(Class<T> entityClass, Object primaryKey) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.getReference(entityClass, primaryKey);
        }
    }

    @Override
    public Object getReference(String entityName, Object id) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.getReference(entityName, id);
        }
    }

    @Override
    public <T> T getReference(T object) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.getReference(object);
        }
    }

    @Override
    public void flush() {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            emr.session.flush();
        }
    }

    @Override
    public void setFlushMode(FlushModeType flushMode) {
        try (SessionResult emr = acquireSession()) {
            emr.session.setFlushMode(flushMode);
        }
    }

    @Override
    public FlushModeType getFlushMode() {
        try (SessionResult emr = acquireSession()) {
            return emr.session.getFlushMode();
        }
    }

    @Override
    public void lock(Object entity, LockModeType lockMode) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            emr.session.lock(entity, lockMode);
        }
    }

    @Override
    public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            emr.session.lock(entity, lockMode, properties);
        }
    }

    @Override
    public void refresh(Object entity) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            emr.session.refresh(entity);
        }
    }

    @Override
    public void refresh(Object entity, Map<String, Object> properties) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            emr.session.refresh(entity, properties);
        }
    }

    @Override
    public void refresh(Object entity, LockModeType lockMode) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            emr.session.refresh(entity, lockMode);
        }
    }

    @Override
    public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            emr.session.refresh(entity, lockMode, properties);
        }
    }

    @Override
    public void clear() {
        try (SessionResult emr = acquireSession()) {
            emr.session.clear();
        }
    }

    @Override
    public void detach(Object entity) {
        try (SessionResult emr = acquireSession()) {
            emr.session.detach(entity);
        }
    }

    @Override
    public boolean contains(Object entity) {
        try (SessionResult emr = acquireSession()) {
            return emr.session.contains(entity);
        }
    }

    @Override
    public LockModeType getLockMode(Object entity) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.getLockMode(entity);
        }
    }

    @Override
    public void setProperty(String propertyName, Object value) {
        try (SessionResult emr = acquireSession()) {
            emr.session.setProperty(propertyName, value);
        }
    }

    @Override
    public Map<String, Object> getProperties() {
        try (SessionResult emr = acquireSession()) {
            return emr.session.getProperties();
        }
    }

    @Deprecated
    @Override
    public Query createQuery(String qlString) {
        checkBlocking();
        //TODO: this needs some thought for how it works outside a tx
        try (SessionResult emr = acquireSession()) {
            return emr.session.createQuery(qlString);
        }
    }

    @Override
    public <T> Query<T> createQuery(CriteriaQuery<T> criteriaQuery) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createQuery(criteriaQuery);
        }
    }

    @Deprecated
    @Override
    public Query createQuery(CriteriaUpdate updateQuery) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createQuery(updateQuery);
        }
    }

    @Deprecated
    @Override
    public Query createQuery(CriteriaDelete deleteQuery) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createQuery(deleteQuery);
        }
    }

    @Override
    public <T> Query<T> createQuery(String qlString, Class<T> resultClass) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createQuery(qlString, resultClass);
        }
    }

    @Deprecated
    @Override
    public Query createNamedQuery(String name) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createNamedQuery(name);
        }
    }

    @Override
    public <T> Query<T> createNamedQuery(String name, Class<T> resultClass) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createNamedQuery(name, resultClass);
        }
    }

    @Deprecated
    @Override
    public NativeQuery createNativeQuery(String sqlString) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createNativeQuery(sqlString);
        }
    }

    @Deprecated
    @Override
    public NativeQuery createNativeQuery(String sqlString, Class resultClass) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createNativeQuery(sqlString, resultClass);
        }
    }

    @Deprecated
    @Override
    public NativeQuery createNativeQuery(String sqlString, String resultSetMapping) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createNativeQuery(sqlString, resultSetMapping);
        }
    }

    @Override
    public ProcedureCall createNamedStoredProcedureQuery(String name) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createNamedStoredProcedureQuery(name);
        }
    }

    @Override
    public ProcedureCall createStoredProcedureQuery(String procedureName) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createStoredProcedureQuery(procedureName);
        }
    }

    @Override
    public ProcedureCall createStoredProcedureQuery(String procedureName,
            @SuppressWarnings("rawtypes") Class... resultClasses) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createStoredProcedureQuery(procedureName, resultClasses);
        }
    }

    @Override
    public ProcedureCall createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createStoredProcedureQuery(procedureName, resultSetMappings);
        }
    }

    @Override
    public void joinTransaction() {
        try (SessionResult emr = acquireSession()) {
            emr.session.joinTransaction();
        }
    }

    @Override
    public boolean isJoinedToTransaction() {
        try (SessionResult emr = acquireSession()) {
            return emr.session.isJoinedToTransaction();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> cls) {
        if (cls.isAssignableFrom(Session.class)) {
            return (T) this;
        }
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.unwrap(cls);
        }
    }

    @Override
    public Object getDelegate() {
        try (SessionResult emr = acquireSession()) {
            return emr.session.getDelegate();
        }
    }

    @Override
    public void close() {
        throw new IllegalStateException("Not supported for transaction scoped entity managers");
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public Transaction getTransaction() {
        throw new IllegalStateException("Not supported for JTA entity managers");
    }

    @Override
    public EntityManagerFactory getEntityManagerFactory() {
        return sessionFactory;
    }

    @Override
    public HibernateCriteriaBuilder getCriteriaBuilder() {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.getCriteriaBuilder();
        }
    }

    @Override
    public Metamodel getMetamodel() {
        try (SessionResult emr = acquireSession()) {
            return emr.session.getMetamodel();
        }
    }

    @Override
    public <T> RootGraph<T> createEntityGraph(Class<T> rootType) {
        try (SessionResult emr = acquireSession()) {
            return emr.session.createEntityGraph(rootType);
        }
    }

    @Override
    public RootGraph<?> createEntityGraph(String graphName) {
        try (SessionResult emr = acquireSession()) {
            return emr.session.createEntityGraph(graphName);
        }
    }

    @Override
    public RootGraph<?> getEntityGraph(String graphName) {
        try (SessionResult emr = acquireSession()) {
            return emr.session.getEntityGraph(graphName);
        }
    }

    @Override
    public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
        try (SessionResult emr = acquireSession()) {
            return emr.session.getEntityGraphs(entityClass);
        }
    }

    @Override
    public SharedSessionBuilder sessionWithOptions() {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.sessionWithOptions();
        }
    }

    @Override
    public void setHibernateFlushMode(FlushMode flushMode) {
        try (SessionResult emr = acquireSession()) {
            emr.session.setHibernateFlushMode(flushMode);
        }
    }

    @Override
    public FlushMode getHibernateFlushMode() {
        try (SessionResult emr = acquireSession()) {
            return emr.session.getHibernateFlushMode();
        }
    }

    @Override
    public void setCacheMode(CacheMode cacheMode) {
        try (SessionResult emr = acquireSession()) {
            emr.session.setCacheMode(cacheMode);
        }
    }

    @Override
    public CacheMode getCacheMode() {
        try (SessionResult emr = acquireSession()) {
            return emr.session.getCacheMode();
        }
    }

    @Override
    public CacheStoreMode getCacheStoreMode() {
        try (SessionResult emr = acquireSession()) {
            return emr.session.getCacheStoreMode();
        }
    }

    @Override
    public CacheRetrieveMode getCacheRetrieveMode() {
        try (SessionResult emr = acquireSession()) {
            return emr.session.getCacheRetrieveMode();
        }
    }

    @Override
    public void setCacheStoreMode(CacheStoreMode cacheStoreMode) {
        try (SessionResult emr = acquireSession()) {
            emr.session.setCacheStoreMode(cacheStoreMode);
        }
    }

    @Override
    public void setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
        try (SessionResult emr = acquireSession()) {
            emr.session.setCacheRetrieveMode(cacheRetrieveMode);
        }
    }

    @Override
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    @Override
    public void cancelQuery() throws HibernateException {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.session.cancelQuery();
        }
    }

    @Override
    public boolean isDirty() throws HibernateException {
        try (SessionResult emr = acquireSession()) {
            return emr.session.isDirty();
        }
    }

    @Override
    public boolean isDefaultReadOnly() {
        try (SessionResult emr = acquireSession()) {
            return emr.session.isDefaultReadOnly();
        }
    }

    @Override
    public void setDefaultReadOnly(boolean readOnly) {
        try (SessionResult emr = acquireSession()) {
            emr.session.setDefaultReadOnly(readOnly);
        }
    }

    @Override
    public Object getIdentifier(Object object) {
        try (SessionResult emr = acquireSession()) {
            return emr.session.getIdentifier(object);
        }
    }

    @Override
    public boolean contains(String entityName, Object object) {
        try (SessionResult emr = acquireSession()) {
            return emr.session.contains(entityName, object);
        }
    }

    @Override
    public void evict(Object object) {
        try (SessionResult emr = acquireSession()) {
            emr.session.evict(object);
        }
    }

    @Deprecated
    @Override
    public <T> T load(Class<T> theClass, Object id, LockMode lockMode) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.load(theClass, id, lockMode);
        }
    }

    @Deprecated
    @Override
    public <T> T load(Class<T> theClass, Object id, LockOptions lockOptions) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.load(theClass, id, lockOptions);
        }
    }

    @Deprecated
    @Override
    public Object load(String entityName, Object id, LockMode lockMode) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.load(entityName, id, lockMode);
        }
    }

    @Deprecated
    @Override
    public Object load(String entityName, Object id, LockOptions lockOptions) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.load(entityName, id, lockOptions);
        }
    }

    @Deprecated
    @Override
    public <T> T load(Class<T> theClass, Object id) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.load(theClass, id);
        }
    }

    @Deprecated
    @Override
    public Object load(String entityName, Object id) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.load(entityName, id);
        }
    }

    @Override
    public void load(Object object, Object id) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.session.load(object, id);
        }
    }

    @Deprecated
    @Override
    public void replicate(Object object, ReplicationMode replicationMode) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.session.replicate(object, replicationMode);
        }
    }

    @Deprecated
    @Override
    public void replicate(String entityName, Object object, ReplicationMode replicationMode) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.session.replicate(entityName, object, replicationMode);
        }
    }

    @Deprecated
    @Override
    public Object save(Object object) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.save(object);
        }
    }

    @Deprecated
    @Override
    public Object save(String entityName, Object object) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.save(entityName, object);
        }
    }

    @Deprecated
    @Override
    public void saveOrUpdate(Object object) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.session.saveOrUpdate(object);
        }
    }

    @Deprecated
    @Override
    public void saveOrUpdate(String entityName, Object object) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.session.saveOrUpdate(entityName, object);
        }
    }

    @Deprecated
    @Override
    public void update(Object object) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.session.update(object);
        }
    }

    @Deprecated
    @Override
    public void update(String entityName, Object object) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.session.update(entityName, object);
        }
    }

    @Override
    public <T> T merge(String entityName, T object) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.merge(entityName, object);
        }
    }

    @Override
    public void persist(String entityName, Object object) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.session.persist(entityName, object);
        }
    }

    @Deprecated
    @Override
    public void delete(Object object) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.session.delete(object);
        }
    }

    @Deprecated
    @Override
    public void delete(String entityName, Object object) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.session.delete(entityName, object);
        }
    }

    @Override
    public void lock(Object object, LockMode lockMode) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.session.lock(object, lockMode);
        }
    }

    @Override
    public void lock(Object object, LockOptions lockOptions) {
        try (SessionResult emr = acquireSession()) {
            emr.session.lock(object, lockOptions);
        }
    }

    @Override
    public void lock(String entityName, Object object, LockMode lockMode) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.session.lock(entityName, object, lockMode);
        }
    }

    @Override
    public LockRequest buildLockRequest(LockOptions lockOptions) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.buildLockRequest(lockOptions);
        }
    }

    @Deprecated
    @Override
    public void refresh(String entityName, Object object) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.session.refresh(entityName, object);
        }
    }

    @Override
    public void refresh(Object object, LockMode lockMode) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.session.refresh(object, lockMode);
        }
    }

    @Override
    public void refresh(Object object, LockOptions lockOptions) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.session.refresh(object, lockOptions);
        }
    }

    @Deprecated
    @Override
    public void refresh(String entityName, Object object, LockOptions lockOptions) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.session.refresh(entityName, object, lockOptions);
        }
    }

    @Override
    public LockMode getCurrentLockMode(Object object) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.getCurrentLockMode(object);
        }
    }

    @Override
    public <T> T get(Class<T> entityType, Object id) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.get(entityType, id);
        }
    }

    @Override
    public <T> T get(Class<T> entityType, Object id, LockMode lockMode) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.get(entityType, id, lockMode);
        }
    }

    @Override
    public <T> T get(Class<T> entityType, Object id, LockOptions lockOptions) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.get(entityType, id, lockOptions);
        }
    }

    @Override
    public Object get(String entityName, Object id) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.get(entityName, id);
        }
    }

    @Override
    public Object get(String entityName, Object id, LockMode lockMode) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.get(entityName, id, lockMode);
        }
    }

    @Override
    public Object get(String entityName, Object id, LockOptions lockOptions) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.get(entityName, id, lockOptions);
        }
    }

    @Override
    public String getEntityName(Object object) {
        try (SessionResult emr = acquireSession()) {
            return emr.session.getEntityName(object);
        }
    }

    @Override
    public <T> IdentifierLoadAccess<T> byId(String entityName) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.byId(entityName);
        }
    }

    @Override
    public <T> MultiIdentifierLoadAccess<T> byMultipleIds(Class<T> entityClass) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.byMultipleIds(entityClass);
        }
    }

    @Override
    public <T> MultiIdentifierLoadAccess<T> byMultipleIds(String entityName) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.byMultipleIds(entityName);
        }
    }

    @Override
    public <T> IdentifierLoadAccess<T> byId(Class<T> entityClass) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.byId(entityClass);
        }
    }

    @Override
    public <T> NaturalIdLoadAccess<T> byNaturalId(String entityName) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.byNaturalId(entityName);
        }
    }

    @Override
    public <T> NaturalIdLoadAccess<T> byNaturalId(Class<T> entityClass) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.byNaturalId(entityClass);
        }
    }

    @Override
    public <T> SimpleNaturalIdLoadAccess<T> bySimpleNaturalId(String entityName) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.bySimpleNaturalId(entityName);
        }
    }

    @Override
    public <T> SimpleNaturalIdLoadAccess<T> bySimpleNaturalId(Class<T> entityClass) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.bySimpleNaturalId(entityClass);
        }
    }

    @Override
    public Filter enableFilter(String filterName) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.enableFilter(filterName);
        }
    }

    @Override
    public Filter getEnabledFilter(String filterName) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.getEnabledFilter(filterName);
        }
    }

    @Override
    public void disableFilter(String filterName) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.session.disableFilter(filterName);
        }
    }

    @Override
    public SessionStatistics getStatistics() {
        try (SessionResult emr = acquireSession()) {
            return emr.session.getStatistics();
        }
    }

    @Override
    public boolean isReadOnly(Object entityOrProxy) {
        try (SessionResult emr = acquireSession()) {
            return emr.session.isReadOnly(entityOrProxy);
        }
    }

    @Override
    public void setReadOnly(Object entityOrProxy, boolean readOnly) {
        try (SessionResult emr = acquireSession()) {
            emr.session.setReadOnly(entityOrProxy, readOnly);
        }
    }

    @Override
    public boolean isFetchProfileEnabled(String name) throws UnknownProfileException {
        try (SessionResult emr = acquireSession()) {
            return emr.session.isFetchProfileEnabled(name);
        }
    }

    @Override
    public void enableFetchProfile(String name) throws UnknownProfileException {
        try (SessionResult emr = acquireSession()) {
            emr.session.enableFetchProfile(name);
        }
    }

    @Override
    public void disableFetchProfile(String name) throws UnknownProfileException {
        try (SessionResult emr = acquireSession()) {
            emr.session.disableFetchProfile(name);
        }
    }

    @Override
    public LobHelper getLobHelper() {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.getLobHelper();
        }
    }

    @Override
    public void addEventListeners(SessionEventListener... listeners) {
        try (SessionResult emr = acquireSession()) {
            emr.session.addEventListeners(listeners);
        }
    }

    @Override
    public String getTenantIdentifier() {
        try (SessionResult emr = acquireSession()) {
            return emr.session.getTenantIdentifier();
        }
    }

    @Override
    public boolean isConnected() {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.isConnected();
        }
    }

    @Override
    public Transaction beginTransaction() {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.beginTransaction();
        }
    }

    @Deprecated
    @Override
    public Query getNamedQuery(String queryName) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.getNamedQuery(queryName);
        }
    }

    @Override
    public ProcedureCall getNamedProcedureCall(String name) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.getNamedProcedureCall(name);
        }
    }

    @Override
    public ProcedureCall createStoredProcedureCall(String procedureName) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createStoredProcedureCall(procedureName);
        }
    }

    @Override
    public ProcedureCall createStoredProcedureCall(String procedureName, Class... resultClasses) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createStoredProcedureCall(procedureName, resultClasses);
        }
    }

    @Override
    public ProcedureCall createStoredProcedureCall(String procedureName, String... resultSetMappings) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createStoredProcedureCall(procedureName, resultSetMappings);
        }
    }

    @Override
    public Integer getJdbcBatchSize() {
        try (SessionResult emr = acquireSession()) {
            return emr.session.getJdbcBatchSize();
        }
    }

    @Override
    public void setJdbcBatchSize(Integer jdbcBatchSize) {
        try (SessionResult emr = acquireSession()) {
            emr.session.setJdbcBatchSize(jdbcBatchSize);
        }
    }

    @Override
    public void doWork(Work work) throws HibernateException {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.session.doWork(work);
        }
    }

    @Override
    public <T> T doReturningWork(ReturningWork<T> work) throws HibernateException {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.doReturningWork(work);
        }
    }

    @Deprecated
    @Override
    public NativeQuery getNamedNativeQuery(String name) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.getNamedNativeQuery(name);
        }
    }

    @Override
    public <R> NativeQuery<R> createNativeQuery(String sqlString, Class<R> resultClass, String tableAlias) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createNativeQuery(sqlString, resultClass, tableAlias);
        }
    }

    @Override
    public <R> NativeQuery<R> createNativeQuery(String sqlString, String resultSetMappingName, Class<R> resultClass) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createNativeQuery(sqlString, resultSetMappingName, resultClass);
        }
    }

    @Override
    public SelectionQuery<?> createSelectionQuery(String hqlString) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createSelectionQuery(hqlString);
        }
    }

    @Override
    public <R> SelectionQuery<R> createSelectionQuery(String hqlString, Class<R> resultType) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createSelectionQuery(hqlString, resultType);
        }
    }

    @Override
    public <R> SelectionQuery<R> createSelectionQuery(CriteriaQuery<R> criteria) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createSelectionQuery(criteria);
        }
    }

    @Override
    public MutationQuery createMutationQuery(String hqlString) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createMutationQuery(hqlString);
        }
    }

    @Override
    public MutationQuery createMutationQuery(CriteriaUpdate updateQuery) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createMutationQuery(updateQuery);
        }
    }

    @Override
    public MutationQuery createMutationQuery(CriteriaDelete deleteQuery) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createMutationQuery(deleteQuery);
        }
    }

    @Override
    public MutationQuery createMutationQuery(JpaCriteriaInsertSelect insertSelect) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createMutationQuery(insertSelect);
        }
    }

    @Override
    public MutationQuery createNativeMutationQuery(String sqlString) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createNativeMutationQuery(sqlString);
        }
    }

    @Override
    public SelectionQuery<?> createNamedSelectionQuery(String name) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createNamedSelectionQuery(name);
        }
    }

    @Override
    public <R> SelectionQuery<R> createNamedSelectionQuery(String name, Class<R> resultType) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createNamedSelectionQuery(name, resultType);
        }
    }

    @Override
    public MutationQuery createNamedMutationQuery(String name) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createNamedMutationQuery(name);
        }
    }

    @Deprecated
    @Override
    public NativeQuery getNamedNativeQuery(String name, String resultSetMapping) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.getNamedNativeQuery(name, resultSetMapping);
        }
    }

    @Override
    public <T> NaturalIdMultiLoadAccess<T> byMultipleNaturalId(Class<T> entityClass) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.byMultipleNaturalId(entityClass);
        }
    }

    @Override
    public <T> NaturalIdMultiLoadAccess<T> byMultipleNaturalId(String entityName) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.byMultipleNaturalId(entityName);
        }
    }

    static class SessionResult implements AutoCloseable {

        final Session session;
        final boolean closeOnEnd;
        final boolean allowModification;

        SessionResult(Session session, boolean closeOnEnd, boolean allowModification) {
            this.session = session;
            this.closeOnEnd = closeOnEnd;
            this.allowModification = allowModification;
        }

        @Override
        public void close() {
            if (closeOnEnd) {
                session.close();
            }
        }
    }

    @Override
    public <T> RootGraph<T> createEntityGraph(Class<T> rootType, String graphName) {
        try (SessionResult emr = acquireSession()) {
            return emr.session.createEntityGraph(rootType, graphName);
        }
    }

    @Override
    public SessionFactory getFactory() {
        return sessionFactory;
    }

    @Override
    public int getFetchBatchSize() {
        try (SessionResult emr = acquireSession()) {
            return emr.session.getFetchBatchSize();
        }
    }

    @Override
    public void setFetchBatchSize(int batchSize) {
        try (SessionResult emr = acquireSession()) {
            emr.session.setFetchBatchSize(batchSize);
        }
    }

    @Override
    public boolean isSubselectFetchingEnabled() {
        try (SessionResult emr = acquireSession()) {
            return emr.session.isSubselectFetchingEnabled();
        }
    }

    @Override
    public void setSubselectFetchingEnabled(boolean enabled) {
        try (SessionResult emr = acquireSession()) {
            emr.session.setSubselectFetchingEnabled(enabled);
        }
    }
}
