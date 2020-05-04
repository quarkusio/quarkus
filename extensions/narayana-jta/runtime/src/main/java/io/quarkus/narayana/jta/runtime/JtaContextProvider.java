package io.quarkus.narayana.jta.runtime;

import java.util.Map;

import javax.enterprise.inject.spi.CDI;
import javax.transaction.InvalidTransactionException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;
import org.jboss.logging.Logger;

public class JtaContextProvider implements ThreadContextProvider {
    private static final Logger logger = Logger.getLogger(JtaContextProvider.class);
    private volatile TransactionManager delegate;

    public JtaContextProvider() {
    }

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {

        Transaction capturedTransaction = currentTransaction();
        return () -> {
            // remove/restore current transaction
            Transaction currentTransaction = currentTransaction();
            if (capturedTransaction != null) {
                if (capturedTransaction != currentTransaction) {
                    if (currentTransaction != null)
                        suspendTransaction();
                    resumeTransaction(capturedTransaction);
                } else {
                    // else we're already in the right transaction
                    logger.debugf("Keeping current transaction ", currentTransaction);
                }
            } else if (currentTransaction != null) {
                suspendTransaction();
            }
            return () -> {
                if (capturedTransaction != null) {
                    if (capturedTransaction != currentTransaction) {
                        suspendTransaction();
                        if (currentTransaction != null)
                            resumeTransaction(currentTransaction);
                    } else {
                        // else we already were in the right transaction
                        logger.debugf("Keeping (not restoring) current transaction ", currentTransaction);
                    }
                } else if (currentTransaction != null) {
                    resumeTransaction(currentTransaction);
                }
            };
        };
    }

    private void resumeTransaction(Transaction transaction) {
        try {
            logger.debugf("Resuming transaction %s", transaction);
            getDelegate().resume(transaction);
        } catch (InvalidTransactionException | IllegalStateException | SystemException e) {
            throw new RuntimeException(e);
        }
    }

    private TransactionManager getDelegate() {
        if (delegate == null) {
            delegate = CDI.current().select(TransactionManager.class).get();
        }
        return delegate;
    }

    private void suspendTransaction() {
        try {
            Transaction t = getDelegate().suspend();
            logger.debugf("Suspending transaction %s", t);
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

    private Transaction currentTransaction() {
        try {
            return getDelegate().getTransaction();
        } catch (SystemException e) {
            logger.error("Failed to get current transaction", e);
            return null;
        }
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {

        return new ThreadContextSnapshot() {
            @Override
            public ThreadContextController begin() {
                // remove/restore current transaction
                Transaction currentTransaction = JtaContextProvider.this.currentTransaction();
                if (currentTransaction != null) {
                    JtaContextProvider.this.suspendTransaction();
                }
                return new ThreadContextController() {
                    @Override
                    public void endContext() throws IllegalStateException {
                        if (currentTransaction != null) {
                            JtaContextProvider.this.resumeTransaction(currentTransaction);
                        }
                    }
                };
            }
        };
    }

    @Override
    public String getThreadContextType() {
        return ThreadContext.TRANSACTION;
    }

}
