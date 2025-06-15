package io.quarkus.narayana.jta;

import java.util.function.Function;

/**
 * Enum that can be used to control the transaction behaviour in the presence or absence of an existing transaction.
 *
 * @see QuarkusTransaction#joiningExisting()
 * @see QuarkusTransaction#requiringNew()
 * @see QuarkusTransaction#disallowingExisting()
 * @see QuarkusTransaction#suspendingExisting()
 * @see QuarkusTransaction#runner(TransactionSemantics)
 */
public enum TransactionSemantics {

    /**
     * If a transaction is already associated with the current thread a {@link QuarkusTransactionException} will be
     * thrown, otherwise a new transaction is started, and follows all the normal lifecycle rules.
     *
     * @see QuarkusTransaction#disallowingExisting()
     * @see QuarkusTransaction#runner(TransactionSemantics)
     */
    DISALLOW_EXISTING,

    /**
     * If no transaction is active then a new transaction will be started, and committed when the method ends. If an
     * exception is thrown the exception handler registered by
     * {@link TransactionRunnerOptions#exceptionHandler(Function)} will be called to decide if the TX should be
     * committed or rolled back.
     * <p>
     * If an existing transaction is active then the method is run in the context of the existing transaction. If an
     * exception is thrown the exception handler will be called, however a result of
     * {@link TransactionExceptionResult#ROLLBACK} will result in the TX marked as rollback only, while a result of
     * {@link TransactionExceptionResult#COMMIT} will result in no action being taken.
     *
     * @see QuarkusTransaction#joiningExisting()
     * @see QuarkusTransaction#runner(TransactionSemantics)
     */
    JOIN_EXISTING,

    /**
     * If an existing transaction is already associated with the current thread then the transaction is suspended, then
     * a new transaction is started which follows all the normal lifecycle rules, and when it's complete the original
     * transaction is resumed.
     * <p>
     * Otherwise, a new transaction is started, and follows all the normal lifecycle rules.
     *
     * @see QuarkusTransaction#requiringNew()
     * @see QuarkusTransaction#runner(TransactionSemantics)
     */
    REQUIRE_NEW,

    /**
     * If no transaction is active then these semantics are basically a no-op.
     * <p>
     * If a transaction is active then it is suspended, and resumed after the task is run.
     * <p>
     * The exception handler will never be consulted when these semantics are in use, specifying both an exception
     * handler and these semantics are considered an error.
     * <p>
     * These semantics allows for code to easily be run outside the scope of a transaction.
     *
     * @see QuarkusTransaction#suspendingExisting()
     * @see QuarkusTransaction#runner(TransactionSemantics)
     */
    SUSPEND_EXISTING

}
