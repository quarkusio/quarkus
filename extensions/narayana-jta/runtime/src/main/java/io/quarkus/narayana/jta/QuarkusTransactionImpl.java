package io.quarkus.narayana.jta;

import java.util.concurrent.Callable;

import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.narayana.jta.runtime.TransactionManagerConfiguration;

class QuarkusTransactionImpl {

    private static final Logger log = Logger.getLogger(QuarkusTransactionImpl.class);
    private static TransactionManager cachedTransactionManager;
    private static UserTransaction cachedUserTransaction;

    public static <T> T call(RunOptions options, Callable<T> task) {
        switch (options.semantic) {
            case REQUIRE_NEW:
                return callRequireNew(options, task);
            case DISALLOW_EXISTING:
                return callDisallowExisting(options, task);
            case JOIN_EXISTING:
                return callJoinExisting(options, task);
            case SUSPEND_EXISTING:
                return callSuspendExisting(options, task);
        }
        throw new IllegalArgumentException("Unknown semantic");
    }

    private static <T> T callSuspendExisting(RunOptions options, Callable<T> task) {
        if (options.exceptionHandler != null) {
            throw new IllegalStateException("Cannot specify both an exception handler and SUSPEND_EXISTING");
        }
        TransactionManager transactionManager = getTransactionManager();
        Transaction transaction = null;
        try {
            if (isTransactionActive()) {
                transaction = transactionManager.suspend();
            }
            T result = task.call();
            if (transaction != null) {
                try {
                    transactionManager.resume(transaction);
                    transaction = null;
                } catch (Exception e) {
                    throw new QuarkusTransactionException(e);
                }
            }
            return result;
        } catch (Exception e) {
            if (transaction != null) {
                try {
                    transactionManager.resume(transaction);
                } catch (Exception ex) {
                    e.addSuppressed(ex);
                }
            }
            if (e instanceof QuarkusTransactionException) {
                throw (QuarkusTransactionException) e;
            }
            throw new QuarkusTransactionException(e);
        }
    }

    private static <T> T callJoinExisting(RunOptions options, Callable<T> task) {
        if (isTransactionActive()) {
            return callInTheirTx(options, task);
        } else {
            return callInOurTx(options, task);
        }
    }

    private static boolean isTransactionActive() {
        try {
            int status = getUserTransaction().getStatus();
            return status != Status.STATUS_NO_TRANSACTION;
        } catch (SystemException e) {
            throw new QuarkusTransactionException(e);
        }
    }

    private static <T> T callDisallowExisting(RunOptions options, Callable<T> task) {
        if (isTransactionActive()) {
            throw new QuarkusTransactionException(new IllegalStateException("Transaction already active"));
        }
        return callInOurTx(options, task);
    }

    private static <T> T callRequireNew(RunOptions options, Callable<T> task) {
        TransactionManager transactionManager = getTransactionManager();
        Transaction transaction = null;
        try {
            if (isTransactionActive()) {
                transaction = transactionManager.suspend();
            }
            T result = callInOurTx(options, task);
            if (transaction != null) {
                try {
                    transactionManager.resume(transaction);
                    transaction = null;
                } catch (Exception e) {
                    throw new QuarkusTransactionException(e);
                }
            }
            return result;
        } catch (Exception e) {
            if (transaction != null) {
                try {
                    transactionManager.resume(transaction);
                } catch (Exception ex) {
                    e.addSuppressed(ex);
                }
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new QuarkusTransactionException(e);
        }
    }

    private static <T> T callInOurTx(RunOptions options, Callable<T> task) {
        begin(options);
        try {
            T ret;
            try {
                ret = task.call();
            } catch (Throwable t) {
                RunOptions.ExceptionResult handling = RunOptions.ExceptionResult.ROLLBACK;
                if (options.exceptionHandler != null) {
                    handling = options.exceptionHandler.apply(t);
                }
                if (handling == RunOptions.ExceptionResult.ROLLBACK) {
                    getUserTransaction().rollback();
                } else {
                    getUserTransaction().commit();
                }
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                } else {
                    throw new QuarkusTransactionException(t);
                }
            }
            try {
                getUserTransaction().commit();
            } catch (Throwable t) {
                throw new QuarkusTransactionException(t);
            }
            return ret;
        } catch (SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException t) {
            try {
                getUserTransaction().rollback();
            } catch (Throwable e) {
                t.addSuppressed(e);
            }
            throw new QuarkusTransactionException(t);
        }
    }

    private static <T> T callInTheirTx(RunOptions options, Callable<T> task) {
        try {
            T ret;
            try {
                ret = task.call();
            } catch (Throwable t) {
                RunOptions.ExceptionResult handling = RunOptions.ExceptionResult.ROLLBACK;
                if (options.exceptionHandler != null) {
                    handling = options.exceptionHandler.apply(t);
                }
                if (handling == RunOptions.ExceptionResult.ROLLBACK) {
                    getUserTransaction().setRollbackOnly();
                }
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                } else {
                    throw new QuarkusTransactionException(t);
                }
            }
            return ret;
        } catch (SystemException t) {
            try {
                getUserTransaction().rollback();
            } catch (Throwable e) {
                t.addSuppressed(e);
            }
            throw new QuarkusTransactionException(t);
        }
    }

    private static void begin(RunOptions options) {
        int timeout = options != null ? options.timeout : 0;
        try {
            if (timeout > 0) {
                getUserTransaction().setTransactionTimeout(timeout);
            }
            getUserTransaction().begin();
        } catch (NotSupportedException | SystemException e) {
            throw new QuarkusTransactionException(e);
        } finally {
            if (timeout > 0) {
                try {
                    getUserTransaction().setTransactionTimeout(
                            (int) Arc.container().instance(TransactionManagerConfiguration.class)
                                    .get().defaultTransactionTimeout.toSeconds());
                } catch (SystemException e) {
                    log.error("Failed to reset transaction timeout", e);
                }
            }
        }
    }

    static void rollback() {
        try {
            getUserTransaction().rollback();
        } catch (SystemException e) {
            throw new QuarkusTransactionException(e);
        }
    }

    static void commit() {
        try {
            getUserTransaction().commit();
        } catch (SystemException | RollbackException | HeuristicMixedException | HeuristicRollbackException e) {
            throw new QuarkusTransactionException(e);
        }
    }

    static void setRollbackOnly() {
        try {
            getUserTransaction().setRollbackOnly();
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

    private static jakarta.transaction.UserTransaction getUserTransaction() {
        if (cachedUserTransaction == null) {
            return cachedUserTransaction = com.arjuna.ats.jta.UserTransaction.userTransaction();
        }
        return cachedUserTransaction;
    }

    private static TransactionManager getTransactionManager() {
        if (cachedTransactionManager == null) {
            return cachedTransactionManager = com.arjuna.ats.jta.TransactionManager
                    .transactionManager();
        }
        return cachedTransactionManager;
    }
}
