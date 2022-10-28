package io.quarkus.narayana.jta;

import java.util.function.Function;

/**
 * Builder interface to allow the transaction semantic of a transaction runner to be selected.
 *
 * @see QuarkusTransaction#runner()
 */
public interface TransactionRunnerSemanticOptions {

    /**
     * Sets the transaction semantic of the runner to {@link TransactionSemantic#JOIN_EXISTING}.
     * <p>
     * If no transaction is active then a new transaction will be started, and committed when the method ends.
     * If an exception is thrown the exception handler registered by
     * {@link TransactionRunnerRunOptions#exceptionHandler(Function)}
     * will be called to decide if the TX should be committed or rolled back.
     * <p>
     * If an existing transaction is active then the method is run in the context of the existing transaction. If an
     * exception is thrown the exception handler will be called, however
     * a result of {@link RunOptions.ExceptionResult#ROLLBACK} will result in the TX marked as rollback only, while a result of
     * {@link RunOptions.ExceptionResult#COMMIT} will result in no action being taken.
     *
     * @return A {@link TransactionRunnerRunOptions} that can be used to set additional options or to run a task with the
     *         selected semantic.
     */
    TransactionRunnerRunOptions joinExisting();

    /**
     * Sets the transaction semantic of the runner to {@link TransactionSemantic#REQUIRE_NEW}.
     * <p>
     * If an existing transaction is already associated with the current thread then the transaction is suspended, and
     * resumed once
     * the current transaction is complete.
     * <p>
     * A new transaction is started after the existing transaction is suspended, and follows all the normal lifecycle rules.
     *
     * @return A {@link TransactionRunnerRunOptions} that can be used to set additional options or to run a task with the
     *         selected semantic.
     */
    TransactionRunnerRunOptions requireNew();

    /**
     * Sets the transaction semantic of the runner to {@link TransactionSemantic#SUSPEND_EXISTING}.
     * <p>
     * If no transaction is active then this semantic is basically a no-op.
     * <p>
     * If a transaction is active then it is suspended, and resumed after the task is run.
     * <p>
     * The exception handler will never be consulted when this semantic is in use, specifying both an exception handler and
     * this semantic is considered an error.
     * <p>
     * This semantic allows for code to easily be run outside the scope of a transaction.
     *
     * @return A {@link TransactionRunnerRunOptions} that can be used to set additional options or to run a task with the
     *         selected semantic.
     */
    TransactionRunnerRunOptions suspendExisting();

    /**
     * Sets the transaction semantic of the runner to {@link TransactionSemantic#DISALLOW_EXISTING}.
     * <p>
     * If a transaction is already associated with the current thread a {@link QuarkusTransactionException} will be thrown,
     * otherwise a new transaction is started, and follows all the normal lifecycle rules.
     *
     * @return A {@link TransactionRunnerRunOptions} that can be used to set additional options or to run a task with the
     *         selected semantic.
     */
    TransactionRunnerRunOptions disallowExisting();

    /**
     * Sets the transaction semantic of the runner to the given value.
     *
     * @return A {@link TransactionRunnerRunOptions} that can be used to set additional options or to run a task with the
     *         selected semantic.
     */
    TransactionRunnerRunOptions semantic(TransactionSemantic semantic);

}
