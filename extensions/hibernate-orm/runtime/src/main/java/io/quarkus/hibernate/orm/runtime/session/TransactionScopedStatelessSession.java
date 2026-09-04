package io.quarkus.hibernate.orm.runtime.session;

import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.inject.Instance;
import jakarta.persistence.TransactionRequiredException;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.StatelessSessionLazyDelegator;

import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.RequestScopedStatelessSessionHolder;
import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.runtime.BlockingOperationNotAllowedException;

/**
 * A transaction-scoped {@link StatelessSession} proxy that resolves the real session on each method call.
 * <p>
 * Extends Hibernate's {@link StatelessSessionLazyDelegator} so that new methods added to the {@link StatelessSession}
 * interface by Hibernate ORM are automatically delegated without requiring Quarkus changes.
 * Only methods that need Quarkus-specific behavior (IO-thread guard, transaction requirement,
 * or special lifecycle semantics) are overridden here.
 */
public class TransactionScopedStatelessSession extends StatelessSessionLazyDelegator {

    protected static final String TRANSACTION_IS_NOT_ACTIVE = "Transaction is not active, consider adding @Transactional to your method to automatically activate one.";

    private final TransactionManager transactionManager;
    private final TransactionSynchronizationRegistry transactionSynchronizationRegistry;
    private final SessionFactory sessionFactory;
    private final JTAStatelessSessionOpener jtaSessionOpener;
    private final String unitName;
    private final String sessionKey;
    private final boolean requestScopedSessionEnabled;
    private final boolean requestScopedStatelessSessionAllowWrite;
    private final Instance<RequestScopedStatelessSessionHolder> requestScopedSessions;

    public TransactionScopedStatelessSession(
            TransactionManager transactionManager,
            TransactionSynchronizationRegistry transactionSynchronizationRegistry,
            SessionFactory sessionFactory,
            String unitName,
            boolean requestScopedSessionEnabled,
            boolean requestScopedStatelessSessionAllowWrite,
            Instance<RequestScopedStatelessSessionHolder> requestScopedSessions) {
        this.transactionManager = transactionManager;
        this.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
        this.sessionFactory = sessionFactory;
        this.jtaSessionOpener = JTAStatelessSessionOpener.create(sessionFactory);
        this.unitName = unitName;
        this.sessionKey = TransactionScopedStatelessSession.class.getSimpleName() + "-" + unitName;
        this.requestScopedSessionEnabled = requestScopedSessionEnabled;
        this.requestScopedStatelessSessionAllowWrite = requestScopedStatelessSessionAllowWrite;
        this.requestScopedSessions = requestScopedSessions;
    }

    @Override
    public StatelessSession delegate() {
        return acquireSession();
    }

    public StatelessSession getDelegateForMutation() {
        if (isInTransaction() || requestScopedStatelessSessionAllowWrite) {
            return delegate();
        }
        throw new TransactionRequiredException(TRANSACTION_IS_NOT_ACTIVE);
    }

    StatelessSession acquireSession() {
        checkBlocking();
        if (isInTransaction()) {
            StatelessSession session = (StatelessSession) transactionSynchronizationRegistry.getResource(sessionKey);
            if (session != null) {
                return session;
            }
            StatelessSession newSession = jtaSessionOpener.openSession();
            // The session has automatically joined the JTA transaction when it was constructed.
            transactionSynchronizationRegistry.putResource(sessionKey, newSession);
            return newSession;
        } else if (requestScopedSessionEnabled) {
            if (Arc.container().requestContext().isActive()) {
                return requestScopedSessions.get().getOrCreateSession(unitName, sessionFactory);
            } else {
                throw new ContextNotActiveException(
                        "Cannot use the StatelessSession because neither a transaction nor a CDI request context is active."
                                + " Consider adding @Transactional to your method to automatically activate a transaction,"
                                + " or @ActivateRequestContext if you have valid reasons not to use transactions.");
            }
        } else {
            throw new ContextNotActiveException(
                    "Cannot use the StatelessSession because no transaction is active."
                            + " Consider adding @Transactional to your method to automatically activate a transaction,"
                            + " or set '" + HibernateOrmRuntimeConfig.extensionPropertyKey("request-scoped.enabled")
                            + "' to 'true' if you have valid reasons not to use transactions.");
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
    public <T> T unwrap(Class<T> type) {
        if (type.isAssignableFrom(StatelessSession.class)) {
            return (T) this;
        }
        return delegate().unwrap(type);
    }

    // -------------------------------------------------------------------------
    // Factory accessor — return directly without acquiring a session
    // -------------------------------------------------------------------------

    @Override
    public SessionFactory getFactory() {
        return sessionFactory;
    }

    // -------------------------------------------------------------------------
    // Methods requiring an active transaction
    // -------------------------------------------------------------------------

    @Override
    public void inTransaction(Consumer<? super Transaction> action) {
        getDelegateForMutation().inTransaction(action);
    }

    @Override
    public <R> R fromTransaction(Function<? super Transaction, R> action) {
        return getDelegateForMutation().fromTransaction(action);
    }

    @Override
    public Object insert(Object entity) {
        return getDelegateForMutation().insert(entity);
    }

    @Override
    public Object insert(String entityName, Object entity) {
        return getDelegateForMutation().insert(entityName, entity);
    }

    @Override
    public void insertMultiple(java.util.List<?> entities) {
        getDelegateForMutation().insertMultiple(entities);
    }

    @Deprecated
    @Override
    public void update(Object entity) {
        getDelegateForMutation().update(entity);
    }

    @Deprecated
    @Override
    public void update(String entityName, Object entity) {
        getDelegateForMutation().update(entityName, entity);
    }

    @Override
    public void updateMultiple(java.util.List<?> entities) {
        getDelegateForMutation().updateMultiple(entities);
    }

    @Deprecated
    @Override
    public void delete(Object entity) {
        getDelegateForMutation().delete(entity);
    }

    @Deprecated
    @Override
    public void delete(String entityName, Object entity) {
        getDelegateForMutation().delete(entityName, entity);
    }

    @Override
    public void deleteMultiple(java.util.List<?> entities) {
        getDelegateForMutation().deleteMultiple(entities);
    }

    @Override
    public void upsert(Object entity) {
        getDelegateForMutation().upsert(entity);
    }

    @Override
    public void upsert(String entityName, Object entity) {
        getDelegateForMutation().upsert(entityName, entity);
    }

    @Override
    public void upsertMultiple(java.util.List<?> entities) {
        getDelegateForMutation().upsertMultiple(entities);
    }

    @Override
    public void refresh(Object entity) {
        getDelegateForMutation().refresh(entity);
    }

    @Deprecated
    @Override
    public void refresh(String entityName, Object entity) {
        getDelegateForMutation().refresh(entityName, entity);
    }

    @Override
    public void refresh(Object entity, org.hibernate.LockMode lockMode) {
        getDelegateForMutation().refresh(entity, lockMode);
    }

    @Override
    public void refresh(String entityName, Object entity, org.hibernate.LockMode lockMode) {
        getDelegateForMutation().refresh(entityName, entity, lockMode);
    }
}
