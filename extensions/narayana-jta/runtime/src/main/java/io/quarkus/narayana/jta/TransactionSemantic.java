package io.quarkus.narayana.jta;

import java.util.function.Function;

/**
 * Enum that can be used to control the transaction behaviour in the presence or absence of an existing transaction.
 *
 * @see QuarkusTransaction#runner(TransactionSemantic)
 */
public enum TransactionSemantic {

    /**
     * If a transaction is already associated with the current thread a {@link QuarkusTransactionException} will be thrown,
     * otherwise a new transaction is started, and follows all the normal lifecycle rules.
     */
    DISALLOW_EXISTING,

    /**
     * If no transaction is active then a new transaction will be started, and committed when the method ends.
     * If an exception is thrown the exception handler registered by
     * {@link TransactionRunnerOptions#exceptionHandler(Function)} will be called to
     * decide if the TX should be committed or rolled back.
     * <p>
     * If an existing transaction is active then the method is run in the context of the existing transaction. If an
     * exception is thrown the exception handler will be called, however
     * a result of {@link TransactionExceptionResult#ROLLBACK} will result in the TX marked as rollback only, while a result of
     * {@link TransactionExceptionResult#COMMIT} will result in no action being taken.
     */
    JOIN_EXISTING,

    /**
     * If an existing transaction is already associated with the current thread then the transaction is suspended,
     * then a new transaction is started which follows all the normal lifecycle rules,
     * and when it's complete the original transaction is resumed.
     * <p>
     * Otherwise, a new transaction is started, and follows all the normal lifecycle rules.
     */
    REQUIRE_NEW,

    /**
     * If no transaction is active then this semantic is basically a no-op.
     * <p>
     * If a transaction is active then it is suspended, and resumed after the task is run.
     * <p>
     * The exception handler will never be consulted when this semantic is in use, specifying both an exception handler and
     * this semantic is considered an error.
     * <p>
     * This semantic allows for code to easily be run outside the scope of a transaction.
     */
    SUSPEND_EXISTING

}
