package io.quarkus.narayana.jta;

import java.util.concurrent.Callable;

import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transactional;

import com.arjuna.ats.jta.UserTransaction;

import io.quarkus.arc.Arc;

/**
 * A simplified transaction interface. While broadly covering the same use cases as {@link jakarta.transaction.UserTransaction},
 * this class is designed to be easier to use. The main features it offers over {@code UserTransaction} are:
 *
 * <ul>
 * <li><b>No Checked Exceptions: </b>All underlying checked exceptions are wrapped in an unchecked
 * {@link QuarkusTransactionException}.</li>
 * <li><b>No Transaction Leaks: </b>Transactions are tied to the request scope, if the scope is destroyed before the transaction
 * is committed the transaction is rolled back. Note that this means this can only currently be used when the request scope is
 * active.</li>
 * <li><b>Per Transaction Timeouts: </b>{@link RunOptions#timeout(int)} can be used to set the new transactions
 * timeout, without affecting the per thread default.</li>
 * <li><b>Lambda Style Transactions: </b> {@link Runnable} and {@link Callable} instances can be run inside the scope of a new
 * transaction.</li>
 * </ul>
 * <p>
 * Note that any checked exception will be wrapped by a {@link QuarkusTransactionException}, while unchecked exceptions are
 * allowed to propagate unchanged.
 */
public interface QuarkusTransaction {

    /**
     * Starts a transaction, using the system default timeout.
     * <p>
     * This transaction will be tied to the current request scope, if it is not committed when the scope is destroyed then it
     * will be rolled back to prevent transaction leaks.
     */
    static void begin() {
        begin(beginOptions());
    }

    /**
     * Starts a transaction, using the system default timeout.
     * <p>
     * This transaction will be tied to the current request scope, if it is not committed when the scope is destroyed then it
     * will be rolled back to prevent transaction leaks.
     *
     * @param options Options that apply to the new transaction
     */
    static void begin(BeginOptions options) {
        RequestScopedTransaction tx = Arc.container().instance(RequestScopedTransaction.class).get();
        tx.begin(options);
    }

    /**
     * Commits the current transaction.
     */
    static void commit() {
        QuarkusTransactionImpl.commit();
    }

    /**
     * Rolls back the current transaction.
     */
    static void rollback() {
        QuarkusTransactionImpl.rollback();
    }

    /**
     * If a transaction is active.
     *
     * @return {@code true} if the transaction is active.
     */
    static boolean isActive() {
        try {
            return UserTransaction.userTransaction().getStatus() != Status.STATUS_NO_TRANSACTION;
        } catch (SystemException e) {
            throw new QuarkusTransactionException(e);
        }
    }

    /**
     * If the transaction is rollback only
     *
     * @return If the transaction has been marked for rollback
     */
    static boolean isRollbackOnly() {
        try {
            return UserTransaction.userTransaction().getStatus() == Status.STATUS_MARKED_ROLLBACK;
        } catch (SystemException e) {
            throw new QuarkusTransactionException(e);
        }
    }

    /**
     * Marks the transaction as rollback only. Operations can still be carried out, however the transaction cannot be
     * successfully committed.
     */
    static void setRollbackOnly() {
        QuarkusTransactionImpl.setRollbackOnly();
    }

    /**
     * Runs a task in a new transaction with the default timeout. This defaults to {@link Transactional.TxType#REQUIRES_NEW}
     * semantics, however alternate semantics can be requested using {@link #run(RunOptions, Runnable)}.
     *
     * @param task The task to run in a transaction
     */
    static void run(Runnable task) {
        run(runOptions(), task);
    }

    /**
     * Runs a task in a new transaction with the default timeout. This defaults to {@link Transactional.TxType#REQUIRES_NEW}
     * semantics, however alternate semantics can be specified using the {@code options} parameter.
     *
     * @param options Options that apply to the new transaction
     * @param task The task to run in a transaction
     */
    static void run(RunOptions options, Runnable task) {
        call(options, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                task.run();
                return null;
            }
        });
    }

    /**
     * Calls a task in a new transaction with the default timeout. This defaults to {@link Transactional.TxType#REQUIRES_NEW}
     * semantics, however alternate semantics can be requested using {@link #call(RunOptions, Callable)}.
     * <p>
     * If the task throws a checked exception it will be wrapped with a {@link QuarkusTransactionException}
     *
     * @param task The task to run in a transaction
     */
    static <T> T call(Callable<T> task) {
        return call(runOptions(), task);
    }

    /**
     * Calls a task in a new transaction with the default timeout. This defaults to {@link Transactional.TxType#REQUIRES_NEW}
     * semantics, however alternate semantics can be requested using {@link #call(RunOptions, Callable)}.
     * <p>
     * If the task throws a checked exception it will be wrapped with a {@link QuarkusTransactionException}
     *
     * @param task The task to run in a transaction
     */
    static <T> T call(RunOptions options, Callable<T> task) {
        return QuarkusTransactionImpl.call(options, task);
    }

    /**
     * @return a new RunOptions
     */
    static RunOptions runOptions() {
        return new RunOptions();
    }

    /**
     * @return a new BeginOptions
     */
    static BeginOptions beginOptions() {
        return new BeginOptions();
    }
}
