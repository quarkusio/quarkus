package io.quarkus.narayana.jta.runtime;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.UserTransaction;

import org.jboss.logging.Logger;

import io.quarkus.narayana.jta.runtime.context.TransactionContext;

public class NotifyingUserTransaction extends TransactionScopedNotifier implements UserTransaction {

    private static final Logger LOG = Logger.getLogger(NotifyingUserTransaction.class);

    private final UserTransaction delegate;

    public NotifyingUserTransaction(UserTransaction delegate) {
        this.delegate = delegate;
    }

    @Override
    public void begin() throws NotSupportedException, SystemException {
        delegate.begin();
        initialized(getTransactionId());
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException,
            IllegalStateException, SystemException {
        TransactionId id = getTransactionId();
        Transaction transaction = currentTransaction();
        beforeDestroyed(id);
        try {
            delegate.commit();
        } finally {
            try {
                TransactionContext.destroyState(transaction);
            } finally {
                destroyed(id);
            }
        }
    }

    @Override
    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        TransactionId id = getTransactionId();
        Transaction transaction = currentTransaction();
        try {
            beforeDestroyed(id);
        } catch (Throwable t) {
            LOG.error("Failed to fire @BeforeDestroyed(TransactionScoped.class)", t);
        }
        try {
            delegate.rollback();
        } finally {
            try {
                TransactionContext.destroyState(transaction);
            } finally {
                destroyed(id);
            }
        }
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        delegate.setRollbackOnly();
    }

    @Override
    public int getStatus() throws SystemException {
        return delegate.getStatus();
    }

    @Override
    public void setTransactionTimeout(int seconds) throws SystemException {
        delegate.setTransactionTimeout(seconds);
    }

}
