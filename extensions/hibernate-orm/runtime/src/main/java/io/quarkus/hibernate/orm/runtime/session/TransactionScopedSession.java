package io.quarkus.hibernate.orm.runtime.session;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.inject.Instance;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockOption;
import jakarta.persistence.RefreshOption;
import jakarta.persistence.TransactionRequiredException;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionLazyDelegator;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.RequestScopedSessionHolder;
import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.BlockingOperationNotAllowedException;

/**
 * A transaction-scoped {@link Session} proxy that resolves the real session on each method call.
 * <p>
 * Extends Hibernate's {@link SessionLazyDelegator} so that new methods added to the {@link Session}
 * interface by Hibernate ORM are automatically delegated without requiring Quarkus changes.
 * Only methods that need Quarkus-specific behavior (IO-thread guard, transaction requirement,
 * or special lifecycle semantics) are overridden here.
 * <p>
 */
public class TransactionScopedSession extends SessionLazyDelegator {

    protected static final String TRANSACTION_IS_NOT_ACTIVE = "Transaction is not active, consider adding @Transactional to your method to automatically activate one.";

    private final TransactionManager transactionManager;
    private final TransactionSynchronizationRegistry transactionSynchronizationRegistry;
    private final SessionFactory sessionFactory;
    private final JTASessionOpener jtaSessionOpener;
    private final String unitName;
    private final String sessionKey;
    private final boolean requestScopedSessionEnabled;
    private final Instance<RequestScopedSessionHolder> requestScopedSessions;

    // Private constructor that uses a one-element array to safely capture `this` in the
    // supplier passed to SessionLazyDelegator before `this` is fully constructed.
    // holder[0] is set to `this` after super() returns; the supplier is never called
    // during construction, so holder[0] is always non-null when the supplier is first invoked.
    private TransactionScopedSession(
            TransactionManager transactionManager,
            TransactionSynchronizationRegistry transactionSynchronizationRegistry,
            SessionFactory sessionFactory,
            String unitName,
            boolean requestScopedSessionEnabled,
            Instance<RequestScopedSessionHolder> requestScopedSessions,
            TransactionScopedSession[] holder) {
        super(() -> holder[0].acquireSession());
        this.transactionManager = transactionManager;
        this.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
        this.sessionFactory = sessionFactory;
        this.jtaSessionOpener = JTASessionOpener.create(sessionFactory);
        this.unitName = unitName;
        this.sessionKey = TransactionScopedSession.class.getSimpleName() + "-" + unitName;
        this.requestScopedSessionEnabled = requestScopedSessionEnabled;
        this.requestScopedSessions = requestScopedSessions;
        holder[0] = this;
    }

    public TransactionScopedSession(
            TransactionManager transactionManager,
            TransactionSynchronizationRegistry transactionSynchronizationRegistry,
            SessionFactory sessionFactory,
            String unitName,
            boolean requestScopedSessionEnabled,
            Instance<RequestScopedSessionHolder> requestScopedSessions) {
        this(transactionManager, transactionSynchronizationRegistry, sessionFactory, unitName,
                requestScopedSessionEnabled, requestScopedSessions, new TransactionScopedSession[1]);
    }

    Session acquireSession() {
        checkBlocking();
        if (isInTransaction()) {
            Session session = (Session) transactionSynchronizationRegistry.getResource(sessionKey);
            if (session != null) {
                return session;
            }
            Session newSession = jtaSessionOpener.openSession();
            // The session has automatically joined the JTA transaction when it was constructed.
            transactionSynchronizationRegistry.putResource(sessionKey, newSession);
            // No need to flush or close the session upon transaction completion:
            // Hibernate ORM itself registers a synchronization that does just that.
            // See:
            // - io.quarkus.hibernate.orm.runtime.boot.FastBootMetadataBuilder.mergeSettings
            // - org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl.joinJtaTransaction
            // - org.hibernate.internal.SessionImpl.beforeTransactionCompletion
            // - org.hibernate.internal.SessionImpl.afterTransactionCompletion
            return newSession;
        } else if (requestScopedSessionEnabled) {
            if (Arc.container().requestContext().isActive()) {
                return requestScopedSessions.get().getOrCreateSession(unitName, sessionFactory);
            } else {
                throw new ContextNotActiveException(
                        "Cannot use the EntityManager/Session because neither a transaction nor a CDI request context is active."
                                + " Consider adding @Transactional to your method to automatically activate a transaction,"
                                + " or @ActivateRequestContext if you have valid reasons not to use transactions.");
            }
        } else {
            throw new ContextNotActiveException(
                    "Cannot use the EntityManager/Session because no transaction is active."
                            + " Consider adding @Transactional to your method to automatically activate a transaction,"
                            + " or set '" + HibernateOrmRuntimeConfig.extensionPropertyKey("request-scoped.enabled")
                            + "' to 'true' if you have valid reasons not to use transactions.");
        }
    }

    private void checkBlocking() {
        if (!BlockingOperationControl.isBlockingAllowed()) {
            throw new BlockingOperationNotAllowedException(
                    "You have attempted to perform a blocking operation on a IO thread. This is not allowed, as blocking the IO thread will cause major performance issues with your application. If you want to perform blocking EntityManager operations make sure you are doing it from a worker thread.");
        }
    }

    public Session getDelegateForMutation() {
        var session = acquireSession();
        if (!isInTransaction()) {
            throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
        }
        return session;
    }

    private void checkActiveTransaction() {
        if (!isInTransaction()) {
            throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
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

    // -------------------------------------------------------------------------
    // Special lifecycle methods — do NOT delegate to the underlying session
    // -------------------------------------------------------------------------

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
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> cls) {
        if (cls.isAssignableFrom(Session.class)) {
            return (T) this;
        }
        return super.unwrap(cls);
    }

    // -------------------------------------------------------------------------
    // Factory accessors — return directly without acquiring a session
    // -------------------------------------------------------------------------

    @Override
    public EntityManagerFactory getEntityManagerFactory() {
        return sessionFactory;
    }

    @Override
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    @Override
    public SessionFactory getFactory() {
        return sessionFactory;
    }

    // -------------------------------------------------------------------------
    // Methods requiring an active transaction
    // Methods that need no extra guards are inherited
    // from SessionLazyDelegator (see bottom of file for full list).
    // -------------------------------------------------------------------------

    @Override
    public void flush() {
        checkActiveTransaction();
        super.flush();
    }

    @Override
    public void inTransaction(Consumer<? super Transaction> action) {
        getDelegateForMutation().inTransaction(action);
    }

    @Override
    public <R> R fromTransaction(Function<? super Transaction, R> action) {
        return getDelegateForMutation().fromTransaction(action);
    }

    @Override
    public void persist(Object entity) {
        getDelegateForMutation().persist(entity);
    }

    @Override
    public void persist(String entityName, Object object) {
        getDelegateForMutation().persist(entityName, object);
    }

    @Override
    public <T> T merge(T entity) {
        return getDelegateForMutation().merge(entity);
    }

    @Override
    public <T> T merge(String entityName, T object) {
        return getDelegateForMutation().merge(entityName, object);
    }

    @Override
    public <T> T merge(T object, EntityGraph<? super T> loadGraph) {
        return getDelegateForMutation().merge(object, loadGraph);
    }

    @Override
    public void remove(Object entity) {
        getDelegateForMutation().remove(entity);
    }

    @Deprecated
    @Override
    public void replicate(Object object, ReplicationMode replicationMode) {
        getDelegateForMutation().replicate(object, replicationMode);
    }

    @Deprecated
    @Override
    public void replicate(String entityName, Object object, ReplicationMode replicationMode) {
        getDelegateForMutation().replicate(entityName, object, replicationMode);
    }

    @Override
    public void lock(Object object, LockMode lockMode) {
        getDelegateForMutation().lock(object, lockMode);
    }

    @Override
    public void lock(Object entity, LockModeType lockMode) {
        getDelegateForMutation().lock(entity, lockMode);
    }

    @Override
    public void lock(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        getDelegateForMutation().lock(entity, lockMode, properties);
    }

    @Override
    public void lock(Object entity, LockModeType lockMode, LockOption... options) {
        getDelegateForMutation().lock(entity, lockMode, options);
    }

    @Override
    public void refresh(Object entity) {
        getDelegateForMutation().refresh(entity);
    }

    @Override
    public void refresh(Object entity, Map<String, Object> properties) {
        getDelegateForMutation().refresh(entity, properties);
    }

    @Override
    public void refresh(Object entity, LockModeType lockMode) {
        getDelegateForMutation().refresh(entity, lockMode);
    }

    @Override
    public void refresh(Object entity, LockModeType lockMode, Map<String, Object> properties) {
        getDelegateForMutation().refresh(entity, lockMode, properties);
    }

    @Override
    public void refresh(Object entity, RefreshOption... options) {
        getDelegateForMutation().refresh(entity, options);
    }

    @Override
    public void refresh(Object object, LockOptions lockOptions) {
        getDelegateForMutation().refresh(object, lockOptions);
    }
}
