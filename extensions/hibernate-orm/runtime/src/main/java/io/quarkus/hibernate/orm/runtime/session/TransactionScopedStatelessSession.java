package io.quarkus.hibernate.orm.runtime.session;

import java.util.List;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.inject.Instance;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.TransactionRequiredException;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.graph.GraphSemantic;
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

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.runtime.RequestScopedStatelessSessionHolder;
import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.BlockingOperationNotAllowedException;

public class TransactionScopedStatelessSession implements StatelessSession {

    protected static final String TRANSACTION_IS_NOT_ACTIVE = "Transaction is not active, consider adding @Transactional to your method to automatically activate one.";

    private final TransactionManager transactionManager;
    private final TransactionSynchronizationRegistry transactionSynchronizationRegistry;
    private final SessionFactory sessionFactory;
    private final JTAStatelessSessionOpener jtaSessionOpener;
    private final String unitName;
    private final String sessionKey;
    private final Instance<RequestScopedStatelessSessionHolder> requestScopedSessions;

    public TransactionScopedStatelessSession(TransactionManager transactionManager,
            TransactionSynchronizationRegistry transactionSynchronizationRegistry,
            SessionFactory sessionFactory,
            String unitName,
            Instance<RequestScopedStatelessSessionHolder> requestScopedSessions) {
        this.transactionManager = transactionManager;
        this.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
        this.sessionFactory = sessionFactory;
        this.jtaSessionOpener = JTAStatelessSessionOpener.create(sessionFactory);
        this.unitName = unitName;
        this.sessionKey = this.getClass().getSimpleName() + "-" + unitName;
        this.requestScopedSessions = requestScopedSessions;
    }

    SessionResult acquireSession() {
        // TODO: this was copied from TransactionScopedSession, but does it need to be the same???
        if (isInTransaction()) {
            StatelessSession session = (StatelessSession) transactionSynchronizationRegistry.getResource(sessionKey);
            if (session != null) {
                return new SessionResult(session, false, true);
            }
            StatelessSession newSession = jtaSessionOpener.openSession();
            // The session has automatically joined the JTA transaction when it was constructed.
            transactionSynchronizationRegistry.putResource(sessionKey, newSession);
            return new SessionResult(newSession, false, true);
        } else if (Arc.container().requestContext().isActive()) {
            RequestScopedStatelessSessionHolder requestScopedSessions = this.requestScopedSessions.get();
            return new SessionResult(requestScopedSessions.getOrCreateSession(unitName, sessionFactory),
                    false, false);
        } else {
            throw new ContextNotActiveException(
                    "Cannot use the StatelessSession because neither a transaction nor a CDI request context is active."
                            + " Consider adding @Transactional to your method to automatically activate a transaction,"
                            + " or @ActivateRequestContext if you have valid reasons not to use transactions.");
        }
    }

    private void checkBlocking() {
        if (!BlockingOperationControl.isBlockingAllowed()) {
            throw new BlockingOperationNotAllowedException(
                    "You have attempted to perform a blocking operation on a IO thread. This is not allowed, as blocking the IO thread will cause major performance issues with your application. If you want to perform blocking StatelessSession operations make sure you are doing it from a worker thread.");
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
    public void refresh(Object entity) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            if (!emr.allowModification) {
                throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
            }
            emr.statelessSession.refresh(entity);
        }
    }

    @Deprecated
    @Override
    public Query createQuery(String qlString) {
        checkBlocking();
        //TODO: this needs some thought for how it works outside a tx
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createQuery(qlString);
        }
    }

    @Override
    public <T> Query<T> createQuery(CriteriaQuery<T> criteriaQuery) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createQuery(criteriaQuery);
        }
    }

    @Deprecated
    @Override
    public Query createQuery(CriteriaUpdate updateQuery) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createQuery(updateQuery);
        }
    }

    @Deprecated
    @Override
    public Query createQuery(CriteriaDelete deleteQuery) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createQuery(deleteQuery);
        }
    }

    @Override
    public <T> Query<T> createQuery(String qlString, Class<T> resultClass) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createQuery(qlString, resultClass);
        }
    }

    @Deprecated
    @Override
    public Query createNamedQuery(String name) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createNamedQuery(name);
        }
    }

    @Override
    public <T> Query<T> createNamedQuery(String name, Class<T> resultClass) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createNamedQuery(name, resultClass);
        }
    }

    @Deprecated
    @Override
    public NativeQuery createNativeQuery(String sqlString) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createNativeQuery(sqlString);
        }
    }

    @Deprecated
    @Override
    public NativeQuery createNativeQuery(String sqlString, Class resultClass) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createNativeQuery(sqlString, resultClass);
        }
    }

    @Deprecated
    @Override
    public NativeQuery createNativeQuery(String sqlString, String resultSetMapping) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createNativeQuery(sqlString, resultSetMapping);
        }
    }

    @Override
    public ProcedureCall createNamedStoredProcedureQuery(String name) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createNamedStoredProcedureQuery(name);
        }
    }

    @Override
    public ProcedureCall createStoredProcedureQuery(String procedureName) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createStoredProcedureQuery(procedureName);
        }
    }

    @Override
    public ProcedureCall createStoredProcedureQuery(String procedureName,
            @SuppressWarnings("rawtypes") Class... resultClasses) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createStoredProcedureQuery(procedureName, resultClasses);
        }
    }

    @Override
    public ProcedureCall createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createStoredProcedureQuery(procedureName, resultSetMappings);
        }
    }

    @Override
    public void joinTransaction() {
        try (SessionResult emr = acquireSession()) {
            emr.statelessSession.joinTransaction();
        }
    }

    @Override
    public boolean isJoinedToTransaction() {
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.isJoinedToTransaction();
        }
    }

    @Override
    public void close() {
        throw new IllegalStateException("Not supported for transaction scoped entity managers");
    }

    @Override
    public Object insert(Object o) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.insert(o);
        }
    }

    @Override
    public Object insert(String s, Object o) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.insert(s, o);
        }
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
    public HibernateCriteriaBuilder getCriteriaBuilder() {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.getCriteriaBuilder();
        }
    }

    @Deprecated
    @Override
    public void update(Object object) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.statelessSession.update(object);
        }
    }

    @Deprecated
    @Override
    public void update(String entityName, Object object) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.statelessSession.update(entityName, object);
        }
    }

    @Deprecated
    @Override
    public void delete(Object object) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.statelessSession.delete(object);
        }
    }

    @Deprecated
    @Override
    public void delete(String entityName, Object object) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.statelessSession.delete(entityName, object);
        }
    }

    @Deprecated
    @Override
    public void refresh(String entityName, Object object) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.statelessSession.refresh(entityName, object);
        }
    }

    @Override
    public void refresh(Object object, LockMode lockMode) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.statelessSession.refresh(object, lockMode);
        }
    }

    @Override
    public void refresh(String s, Object o, LockMode lockMode) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.statelessSession.refresh(s, o, lockMode);
        }
    }

    @Override
    public void fetch(Object o) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.statelessSession.refresh(o);
        }
    }

    @Override
    public <T> T get(Class<T> entityType, Object id) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.get(entityType, id);
        }
    }

    @Override
    public <T> T get(Class<T> entityType, Object id, LockMode lockMode) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.get(entityType, id, lockMode);
        }
    }

    @Override
    public Object get(String entityName, Object id) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.get(entityName, id);
        }
    }

    @Override
    public Object get(String entityName, Object id, LockMode lockMode) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.get(entityName, id, lockMode);
        }
    }

    @Override
    public String getTenantIdentifier() {
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.getTenantIdentifier();
        }
    }

    @Override
    public Object getTenantIdentifierValue() {
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.getTenantIdentifierValue();
        }
    }

    @Override
    public boolean isConnected() {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.isConnected();
        }
    }

    @Override
    public Transaction beginTransaction() {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.beginTransaction();
        }
    }

    @Deprecated
    @Override
    public Query getNamedQuery(String queryName) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.getNamedQuery(queryName);
        }
    }

    @Override
    public ProcedureCall getNamedProcedureCall(String name) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.getNamedProcedureCall(name);
        }
    }

    @Override
    public ProcedureCall createStoredProcedureCall(String procedureName) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createStoredProcedureCall(procedureName);
        }
    }

    @Override
    public ProcedureCall createStoredProcedureCall(String procedureName, Class... resultClasses) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createStoredProcedureCall(procedureName, resultClasses);
        }
    }

    @Override
    public ProcedureCall createStoredProcedureCall(String procedureName, String... resultSetMappings) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createStoredProcedureCall(procedureName, resultSetMappings);
        }
    }

    @Override
    public Integer getJdbcBatchSize() {
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.getJdbcBatchSize();
        }
    }

    @Override
    public void setJdbcBatchSize(Integer jdbcBatchSize) {
        try (SessionResult emr = acquireSession()) {
            emr.statelessSession.setJdbcBatchSize(jdbcBatchSize);
        }
    }

    @Override
    public void doWork(Work work) throws HibernateException {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.statelessSession.doWork(work);
        }
    }

    @Override
    public <T> T doReturningWork(ReturningWork<T> work) throws HibernateException {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.doReturningWork(work);
        }
    }

    @Deprecated
    @Override
    public NativeQuery getNamedNativeQuery(String name) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.getNamedNativeQuery(name);
        }
    }

    @Override
    public <R> NativeQuery<R> createNativeQuery(String sqlString, Class<R> resultClass, String tableAlias) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createNativeQuery(sqlString, resultClass, tableAlias);
        }
    }

    @Override
    public <R> NativeQuery<R> createNativeQuery(String sqlString, String resultSetMappingName, Class<R> resultClass) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createNativeQuery(sqlString, resultSetMappingName, resultClass);
        }
    }

    @Override
    public SelectionQuery<?> createSelectionQuery(String hqlString) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createSelectionQuery(hqlString);
        }
    }

    @Override
    public <R> SelectionQuery<R> createSelectionQuery(String hqlString, Class<R> resultType) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createSelectionQuery(hqlString, resultType);
        }
    }

    @Override
    public <R> SelectionQuery<R> createSelectionQuery(CriteriaQuery<R> criteria) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createSelectionQuery(criteria);
        }
    }

    @Override
    public MutationQuery createMutationQuery(String hqlString) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createMutationQuery(hqlString);
        }
    }

    @Override
    public MutationQuery createMutationQuery(CriteriaUpdate updateQuery) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createMutationQuery(updateQuery);
        }
    }

    @Override
    public MutationQuery createMutationQuery(CriteriaDelete deleteQuery) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createMutationQuery(deleteQuery);
        }
    }

    @Override
    public MutationQuery createMutationQuery(JpaCriteriaInsertSelect insertSelect) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createMutationQuery(insertSelect);
        }
    }

    @Override
    public MutationQuery createNativeMutationQuery(String sqlString) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createNativeMutationQuery(sqlString);
        }
    }

    @Override
    public SelectionQuery<?> createNamedSelectionQuery(String name) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createNamedSelectionQuery(name);
        }
    }

    @Override
    public <R> SelectionQuery<R> createNamedSelectionQuery(String name, Class<R> resultType) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createNamedSelectionQuery(name, resultType);
        }
    }

    @Override
    public MutationQuery createNamedMutationQuery(String name) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createNamedMutationQuery(name);
        }
    }

    @Deprecated
    @Override
    public NativeQuery getNamedNativeQuery(String name, String resultSetMapping) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.getNamedNativeQuery(name, resultSetMapping);
        }
    }

    static class SessionResult implements AutoCloseable {

        final StatelessSession statelessSession;
        final boolean closeOnEnd;
        final boolean allowModification;

        SessionResult(StatelessSession statelessSession, boolean closeOnEnd, boolean allowModification) {
            this.statelessSession = statelessSession;
            this.closeOnEnd = closeOnEnd;
            this.allowModification = allowModification;
        }

        @Override
        public void close() {
            if (closeOnEnd) {
                statelessSession.close();
            }
        }
    }

    @Override
    public <T> RootGraph<T> createEntityGraph(Class<T> rootType) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createEntityGraph(rootType);
        }
    }

    @Override
    public RootGraph<?> createEntityGraph(String graphName) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createEntityGraph(graphName);
        }
    }

    @Override
    public <T> RootGraph<T> createEntityGraph(Class<T> rootType, String graphName) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.createEntityGraph(rootType, graphName);
        }
    }

    @Override
    public RootGraph<?> getEntityGraph(String graphName) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.getEntityGraph(graphName);
        }
    }

    @Override
    public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.getEntityGraphs(entityClass);
        }
    }

    @Override
    public SessionFactory getFactory() {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.getFactory();
        }
    }

    @Override
    public void upsert(Object entity) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.statelessSession.upsert(entity);
        }
    }

    @Override
    public void upsert(String entityName, Object entity) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            emr.statelessSession.upsert(entityName, entity);
        }
    }

    @Override
    public <T> T get(EntityGraph<T> graph, GraphSemantic graphSemantic, Object id) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.get(graph, graphSemantic, id);
        }
    }

    @Override
    public <T> T get(EntityGraph<T> graph, GraphSemantic graphSemantic, Object id, LockMode lockMode) {
        checkBlocking();
        try (SessionResult emr = acquireSession()) {
            return emr.statelessSession.get(graph, graphSemantic, id, lockMode);
        }
    }
}
