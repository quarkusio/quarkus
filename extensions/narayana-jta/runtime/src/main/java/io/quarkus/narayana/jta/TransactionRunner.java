package io.quarkus.narayana.jta;

import java.util.concurrent.Callable;

/**
 * Runs tasks in transactions with pre-defined semantics and options.
 *
 * @see QuarkusTransaction#runner()
 * @see TransactionSemantic
 */
public interface TransactionRunner {

    /**
     * Runs the given runnable,
     * starting/suspending transactions as required by the {@link TransactionRunnerSemanticOptions selected semantic}.
     *
     * @param task A task to run with the selected transaction semantic.
     */
    void run(Runnable task);

    /**
     * Calls the given callable,
     * starting/suspending transactions as required by the {@link TransactionRunnerSemanticOptions selected semantic}.
     * <p>
     * If the task throws a checked exception it will be wrapped with a {@link QuarkusTransactionException}
     *
     * @param task A task to run with the selected transaction semantic.
     * @return The value returned by {@code task.call()}.
     * @throws QuarkusTransactionException If the task throws a checked exception.
     */
    <T> T call(Callable<T> task);

}
