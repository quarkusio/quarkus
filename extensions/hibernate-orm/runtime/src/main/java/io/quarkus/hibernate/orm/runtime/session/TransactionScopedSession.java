package io.quarkus.hibernate.orm.runtime.session;

import java.io.Serializable;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Instance;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TransactionRequiredException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.metamodel.Metamodel;
import javax.transaction.Status;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.IdentifierLoadAccess;
import org.hibernate.LobHelper;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.SessionEventListener;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.Transaction;
import org.hibernate.TypeHelper;
import org.hibernate.UnknownProfileException;
import org.hibernate.graph.RootGraph;
import org.hibernate.jdbc.ReturningWork;
import org.hibernate.jdbc.Work;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.stat.SessionStatistics;

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
        } else {
            //this will throw an exception if the request scope is not active
            //this is expected as either the request scope or an active transaction
            //is required to properly managed the EM lifecycle
            RequestScopedSessionHolder requestScopedSessions = this.requestScopedSessions.get();
            return new SessionResult(requestScopedSessions.getOrCreateSession(unitName, sessionFactory),
                    false, false);
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
    public Object merge(Object entity) {
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

    @Override
    public Query createQuery(CriteriaUpdate updateQuery) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createQuery(updateQuery);
        }
    }

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

    @Override
    public NativeQuery createNativeQuery(String sqlString) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createNativeQuery(sqlString);
        }
    }

    @Override
    public NativeQuery createNativeQuery(String sqlString, Class resultClass) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createNativeQuery(sqlString, resultClass);
        }
    }

    @Override
    public NativeQuery createNativeQuery(String sqlString, String resultSetMapping) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createNativeQuery(sqlString, resultSetMapping);
        }
    }

    @Override
    public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createNamedStoredProcedureQuery(name);
        }
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createStoredProcedureQuery(procedureName);
        }
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createStoredProcedureQuery(procedureName, resultClasses);
        }
    }

    @Override
    public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
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
    public CriteriaBuilder getCriteriaBuilder() {
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
    @Deprecated
    public void setFlushMode(FlushMode flushMode) {
        try (SessionResult emr = acquireSession()) {
            emr.session.setFlushMode(flushMode);
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
    public Serializable getIdentifier(Object object) {
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

    @Override
    public <T> T load(Class<T> theClass, Serializable id, LockMode lockMode) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.load(theClass, id, lockMode);
        }
    }

    @Override
    public <T> T load(Class<T> theClass, Serializable id, LockOptions lockOptions) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.load(theClass, id, lockOptions);
        }
    }

    @Override
    public Object load(String entityName, Serializable id, LockMode lockMode) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.load(entityName, id, lockMode);
        }
    }

    @Override
    public Object load(String entityName, Serializable id, LockOptions lockOptions) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.load(entityName, id, lockOptions);
        }
    }

    @Override
    public <T> T load(Class<T> theClass, Serializable id) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.load(theClass, id);
        }
    }

    @Override
    public Object load(String entityName, Serializable id) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.load(entityName, id);
        }
    }

    @Override
    public void load(Object object, Serializable id) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.session.load(object, id);
        }
    }

    @Override
    public void replicate(Object object, ReplicationMode replicationMode) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.session.replicate(object, replicationMode);
        }
    }

    @Override
    public void replicate(String entityName, Object object, ReplicationMode replicationMode) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.session.replicate(entityName, object, replicationMode);
        }
    }

    @Override
    public Serializable save(Object object) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.save(object);
        }
    }

    @Override
    public Serializable save(String entityName, Object object) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.save(entityName, object);
        }
    }

    @Override
    public void saveOrUpdate(Object object) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.session.saveOrUpdate(object);
        }
    }

    @Override
    public void saveOrUpdate(String entityName, Object object) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.session.saveOrUpdate(entityName, object);
        }
    }

    @Override
    public void update(Object object) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.session.update(object);
        }
    }

    @Override
    public void update(String entityName, Object object) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.session.update(entityName, object);
        }
    }

    @Override
    public Object merge(String entityName, Object object) {
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

    @Override
    public void delete(Object object) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.session.delete(object);
        }
    }

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
    @Deprecated
    public org.hibernate.Query createFilter(Object collection, String queryString) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createFilter(collection, queryString);
        }
    }

    @Override
    public <T> T get(Class<T> entityType, Serializable id) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.get(entityType, id);
        }
    }

    @Override
    public <T> T get(Class<T> entityType, Serializable id, LockMode lockMode) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.get(entityType, id, lockMode);
        }
    }

    @Override
    public <T> T get(Class<T> entityType, Serializable id, LockOptions lockOptions) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.get(entityType, id, lockOptions);
        }
    }

    @Override
    public Object get(String entityName, Serializable id) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.get(entityName, id);
        }
    }

    @Override
    public Object get(String entityName, Serializable id, LockMode lockMode) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.get(entityName, id, lockMode);
        }
    }

    @Override
    public Object get(String entityName, Serializable id, LockOptions lockOptions) {
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
    public IdentifierLoadAccess byId(String entityName) {
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
    public MultiIdentifierLoadAccess byMultipleIds(String entityName) {
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
    public NaturalIdLoadAccess byNaturalId(String entityName) {
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
    public SimpleNaturalIdLoadAccess bySimpleNaturalId(String entityName) {
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
    public Connection disconnect() {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.disconnect();
        }
    }

    @Override
    public void reconnect(Connection connection) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.session.reconnect(connection);
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
    public TypeHelper getTypeHelper() {
        try (SessionResult emr = acquireSession()) {
            return emr.session.getTypeHelper();
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
    public NativeQuery createSQLQuery(String queryString) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createSQLQuery(queryString);
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
    @Deprecated
    public Criteria createCriteria(Class persistentClass) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createCriteria(persistentClass);
        }
    }

    @Override
    @Deprecated
    public Criteria createCriteria(Class persistentClass, String alias) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createCriteria(persistentClass, alias);
        }
    }

    @Override
    @Deprecated
    public Criteria createCriteria(String entityName) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createCriteria(entityName);
        }
    }

    @Override
    @Deprecated
    public Criteria createCriteria(String entityName, String alias) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.createCriteria(entityName, alias);
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

    @Override
    @Deprecated
    public org.hibernate.Query getNamedSQLQuery(String name) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.getNamedSQLQuery(name);
        }
    }

    @Override
    public NativeQuery getNamedNativeQuery(String name) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.session.getNamedNativeQuery(name);
        }
    }

    @Override
    public Session getSession() {
        try (SessionResult emr = acquireSession()) {
            return emr.session.getSession();
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
}
