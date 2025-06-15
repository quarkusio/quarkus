package io.quarkus.narayana.jta;

import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Builder interface to allow various options of a transaction runner to be customized.
 * <p>
 * Note this interface extends {@link TransactionRunner}, so it can be used to run a task directly with
 * {@link #run(Runnable)}/{@link #call(Callable)}, even if no options need to be customized.
 *
 * @see QuarkusTransaction#joiningExisting()
 * @see QuarkusTransaction#requiringNew()
 * @see QuarkusTransaction#disallowingExisting()
 * @see QuarkusTransaction#suspendingExisting()
 * @see QuarkusTransaction#runner(TransactionSemantics)
 */
public interface TransactionRunnerOptions extends TransactionRunner {

    /**
     * Sets the transaction timeout for transactions created by this runner. A value of zero refers to the system
     * default.
     *
     * @throws IllegalArgumentException
     *         If seconds is negative
     *
     * @param seconds
     *        The timeout in seconds
     *
     * @return This builder
     */
    TransactionRunnerOptions timeout(int seconds);

    /**
     * Provides an exception handler that can make a decision to rollback or commit based on the type of exception. If
     * the predicate returns {@link TransactionExceptionResult#ROLLBACK} the transaction is rolled back, otherwise it is
     * committed.
     * <p>
     * This exception will still be propagated to the caller, so this method should not log or perform any other actions
     * other than determine what should happen to the current transaction.
     * <p>
     * By default, the exception is always rolled back.
     *
     * @param handler
     *        The exception handler
     *
     * @return This builder
     */
    TransactionRunnerOptions exceptionHandler(Function<Throwable, TransactionExceptionResult> handler);

}
