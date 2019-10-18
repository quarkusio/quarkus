package io.quarkus.narayana.jta;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.spi.Context;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionScoped;
import javax.transaction.Transactional;

import org.jboss.logging.Logger;

@ApplicationScoped
public class TransactionBeanWithEvents {
    private static final Logger log = Logger.getLogger(TransactionBeanWithEvents.class);

    private static int initializedCount, beforeDestroyedCount, destroyedCount;
    private static int commitCount, rollbackCount;

    @Inject
    private TransactionManager tm;

    static int getInitialized() {
        return initializedCount;
    }

    static int getBeforeDestroyed() {
        return beforeDestroyedCount;
    }

    static int getDestroyed() {
        return destroyedCount;
    }

    static int getCommited() {
        return commitCount;
    }

    static int getRolledBack() {
        return rollbackCount;
    }

    @Transactional
    void doInTransaction(boolean isCommit) {
        log.debug("Running transactional bean method");

        try {
            tm.getTransaction().registerSynchronization(new Synchronization() {
                @Override
                public void beforeCompletion() {
                }

                @Override
                public void afterCompletion(int status) {
                    if (status == Status.STATUS_ROLLEDBACK) {
                        rollbackCount++;
                    } else if (status == Status.STATUS_COMMITTED) {
                        commitCount++;
                    } else {
                        throw new IllegalStateException("Expected commit or rollback on transaction synchronization callback");
                    }
                }
            });
        } catch (Exception e) {
            throw new IllegalStateException("Cannot get transaction to register synchronization on bean call", e);
        }

        if (!isCommit) {
            throw new RuntimeException("Rollback here!");
        }
    }

    void transactionScopeActivated(@Observes @Initialized(TransactionScoped.class) final Object event,
            final BeanManager beanManager) throws SystemException {
        Transaction tx = tm.getTransaction();
        if (tx == null) {
            log.error("@Intialized expects an active transaction");
            throw new IllegalStateException("@Intialized expects an active transaction");
        }
        if (tx.getStatus() != Status.STATUS_ACTIVE) {
            log.error("@Initialized expects transaction is Status.STATUS_ACTIVE");
            throw new IllegalStateException("@Initialized expects transaction is Status.STATUS_ACTIVE");
        }
        Context ctx = null;
        try {
            ctx = beanManager.getContext(TransactionScoped.class);
        } catch (Exception e) {
            log.error("Context on @Initialized is not available");
            throw e;
        }
        if (!ctx.isActive()) {
            log.error("Context on @Initialized has to be active");
            throw new IllegalStateException("Context on @Initialized has to be active");
        }
        if (!(event instanceof Transaction)) {
            log.error("@Intialized scope expects event payload being the " + Transaction.class.getName());
            throw new IllegalStateException("@Intialized scope expects event payload being the " + Transaction.class.getName());
        }

        initializedCount++;
    }

    void transactionScopePreDestroy(@Observes @BeforeDestroyed(TransactionScoped.class) final Object event,
            final BeanManager beanManager) throws SystemException {
        Transaction tx = tm.getTransaction();
        if (tx == null) {
            log.error("@BeforeDestroyed expects an active transaction");
            throw new IllegalStateException("@BeforeDestroyed expects an active transaction");
        }
        Context ctx = null;
        try {
            ctx = beanManager.getContext(TransactionScoped.class);
        } catch (Exception e) {
            log.error("Context on @Initialized is not available");
            throw e;
        }
        if (!ctx.isActive()) {
            log.error("Context on @BeforeDestroyed has to be active");
            throw new IllegalStateException("Context on @BeforeDestroyed has to be active");
        }
        if (!(event instanceof Transaction)) {
            log.error("@Intialized scope expects event payload being the " + Transaction.class.getName());
            throw new IllegalStateException("@Intialized scope expects event payload being the " + Transaction.class.getName());
        }

        beforeDestroyedCount++;
    }

    void transactionScopeDestroyed(@Observes @Destroyed(TransactionScoped.class) final Object event,
            final BeanManager beanManager) throws SystemException {
        Transaction tx = tm.getTransaction();
        if (tx != null)
            throw new IllegalStateException("@Destroyed expects no transaction");
        try {
            Context ctx = beanManager.getContext(TransactionScoped.class);
            throw new IllegalStateException("No bean in context expected but it's " + ctx);
        } catch (final ContextNotActiveException expected) {
        }

        destroyedCount++;
    }
}
