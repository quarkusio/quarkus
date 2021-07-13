package io.quarkus.narayana.jta.runtime;

import java.io.Serializable;

import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionScoped;

import org.jboss.logging.Logger;

/**
 * A delegating transaction manager which receives an instance of Narayana transaction manager
 * and delegates all calls to it.
 * On top of it the implementation adds the CDI events processing for {@link TransactionScoped}.
 */
@Singleton
public class CDIDelegatingTransactionManager implements TransactionManager, Serializable {

    private static final Logger log = Logger.getLogger(CDIDelegatingTransactionManager.class);

    private static final long serialVersionUID = 1598L;

    private final transient com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple delegate;

    /**
     * An {@link Event} that can {@linkplain Event#fire(Object) fire}
     * {@link Transaction}s when the {@linkplain TransactionScoped transaction scope} is initialized.
     */
    @Inject
    @Initialized(TransactionScoped.class)
    Event<Transaction> transactionScopeInitialized;

    /**
     * An {@link Event} that can {@linkplain Event#fire(Object) fire}
     * {@link Object}s before the {@linkplain TransactionScoped transaction scope} is destroyed.
     */
    @Inject
    @BeforeDestroyed(TransactionScoped.class)
    Event<Object> transactionScopeBeforeDestroyed;

    /**
     * An {@link Event} that can {@linkplain Event#fire(Object) fire}
     * {@link Object}s when the {@linkplain TransactionScoped transaction scope} is destroyed.
     */
    @Inject
    @Destroyed(TransactionScoped.class)
    Event<Object> transactionScopeDestroyed;

    /**
     * Delegating transaction manager call to com.arjuna.ats.jta.{@link com.arjuna.ats.jta.TransactionManager}
     */
    public CDIDelegatingTransactionManager() {
        delegate = (com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple) com.arjuna.ats.jta.TransactionManager
                .transactionManager();
    }

    /**
     * Overrides {@link TransactionManager#begin()} to
     * additionally {@linkplain Event#fire(Object) fire} an {@link Object}
     * representing the {@linkplain Initialized initialization}
     * of the {@linkplain TransactionScoped transaction scope}.
     *
     * @see TransactionManager#begin()
     */
    @Override
    public void begin() throws NotSupportedException, SystemException {
        delegate.begin();
        if (this.transactionScopeInitialized != null) {
            this.transactionScopeInitialized.fire(this.getTransaction());
        }
    }

    /**
     * Overrides {@link TransactionManager#commit()} to
     * additionally {@linkplain Event#fire(Object) fire} an {@link Object}
     * representing the {@linkplain BeforeDestroyed before destruction} and
     * the {@linkplain Destroyed destruction}
     * of the {@linkplain TransactionScoped transaction scope}.
     *
     * @see TransactionManager#commit()
     */
    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException,
            IllegalStateException, SystemException {
        if (this.transactionScopeBeforeDestroyed != null) {
            this.transactionScopeBeforeDestroyed.fire(this.getTransaction());
        }

        try {
            delegate.commit();
        } finally {
            if (this.transactionScopeDestroyed != null) {
                this.transactionScopeDestroyed.fire(this.toString());
            }
        }
    }

    /**
     * Overrides {@link TransactionManager#rollback()} to
     * additionally {@linkplain Event#fire(Object) fire} an {@link Object}
     * representing the {@linkplain BeforeDestroyed before destruction} and
     * the {@linkplain Destroyed destruction}
     * of the {@linkplain TransactionScoped transaction scope}.
     *
     * @see TransactionManager#rollback()
     */
    @Override
    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        try {
            if (this.transactionScopeBeforeDestroyed != null) {
                this.transactionScopeBeforeDestroyed.fire(this.getTransaction());
            }
        } catch (Throwable t) {
            log.error("Failed to fire @BeforeDestroyed(TransactionScoped.class)", t);
        }

        try {
            delegate.rollback();
        } finally {
            //we don't need a catch block here, if this one fails we just let the exception propagate
            if (this.transactionScopeDestroyed != null) {
                this.transactionScopeDestroyed.fire(this.toString());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getStatus() throws SystemException {
        return delegate.getStatus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Transaction getTransaction() throws SystemException {
        return delegate.getTransaction();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resume(Transaction transaction) throws InvalidTransactionException, IllegalStateException, SystemException {
        delegate.resume(transaction);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        delegate.setRollbackOnly();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTransactionTimeout(int seconds) throws SystemException {
        delegate.setTransactionTimeout(seconds);
    }

    /**
     * Returns transaction timeout in seconds.
     *
     * @return transaction timeout set currently
     * @throws SystemException on an undefined error
     */
    public int getTransactionTimeout() throws SystemException {
        return delegate.getTimeout();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Transaction suspend() throws SystemException {
        return delegate.suspend();
    }
}
