package io.quarkus.narayana.jta;

import java.util.function.Function;

/**
 * Builder interface to allow a transaction to be customized, including things like timeout and semantics when an
 * existing transaction is present.
 *
 * @deprecated Use {@link QuarkusTransaction#requiringNew()}, {@link QuarkusTransaction#joiningExisting()},
 *             {@link QuarkusTransaction#disallowingExisting()}, {@link QuarkusTransaction#suspendingExisting()} or
 *             {@link QuarkusTransaction#runner(TransactionSemantics)} instead.
 */
@Deprecated
public class RunOptions extends RunOptionsBase {

    /**
     * Sets the transaction timeout for transactions created by this builder. A value of zero refers to the system
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
    public RunOptions timeout(int seconds) {
        setTimeout(seconds);
        return this;
    }

    /**
     * Sets the transaction semantics that is used to determine the action to take if a transaction is already active.
     * <p>
     *
     * @param semantic
     *        The semantic
     *
     * @return This builder
     */
    public RunOptions semantic(Semantic semantic) {
        if (semantic == null) {
            setSemantics(null);
            return this;
        }
        switch (semantic) {
            case DISALLOW_EXISTING:
                setSemantics(TransactionSemantics.DISALLOW_EXISTING);
                break;
            case JOIN_EXISTING:
                setSemantics(TransactionSemantics.JOIN_EXISTING);
                break;
            case REQUIRE_NEW:
                setSemantics(TransactionSemantics.REQUIRE_NEW);
                break;
            case SUSPEND_EXISTING:
                setSemantics(TransactionSemantics.SUSPEND_EXISTING);
                break;
            default:
                throw new IllegalArgumentException("Unsupported value: " + semantic);
        }
        return this;
    }

    /**
     * Provides an exception handler that can make a decision to rollback or commit based on the type of exception. If
     * the predicate returns {@link ExceptionResult#ROLLBACK} the transaction is rolled back, otherwise it is committed.
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
    public RunOptions exceptionHandler(Function<Throwable, ExceptionResult> handler) {
        setExceptionHandler(handler.andThen(result -> {
            if (result == null) {
                return null;
            }
            switch (result) {
                case COMMIT:
                    return TransactionExceptionResult.COMMIT;
                case ROLLBACK:
                    return TransactionExceptionResult.ROLLBACK;
                default:
                    throw new IllegalArgumentException("Unsupported value: " + result);
            }
        }));
        return this;
    }

    /**
     * Enum that can be used to control the transaction behaviour in the presence or absence of an existing transaction.
     */
    public enum Semantic {

        /**
         * If a transaction is already associated with the current thread a {@link QuarkusTransactionException} will be
         * thrown, otherwise a new transaction is started, and follows all the normal lifecycle rules.
         */
        DISALLOW_EXISTING,

        /**
         * <p>
         * If no transaction is active then a new transaction will be started, and committed when the method ends. If an
         * exception is thrown the exception handler registered by {@link #exceptionHandler(Function)} will be called to
         * decide if the TX should be committed or rolled back.
         * <p>
         * If an existing transaction is active then the method is run in the context of the existing transaction. If an
         * exception is thrown the exception handler will be called, however a result of
         * {@link ExceptionResult#ROLLBACK} will result in the TX marked as rollback only, while a result of
         * {@link ExceptionResult#COMMIT} will result in no action being taken.
         */
        JOIN_EXISTING,

        /**
         * This is the default semantic.
         * <p>
         * If an existing transaction is already associated with the current thread then the transaction is suspended,
         * and resumed once the current transaction is complete.
         * <p>
         * A new transaction is started after the existing transaction is suspended, and follows all the normal
         * lifecycle rules.
         * <p>
         */
        REQUIRE_NEW,

        /**
         * If no transaction is active then this semantic is basically a no-op.
         * <p>
         * If a transaction is active then it is suspended, and resumed after the task is run.
         * <p>
         * The exception handler will never be consulted when this semantic is in use, specifying both an exception
         * handler and this semantic is considered an error.
         * <p>
         * This semantic allows for code to easily be run outside the scope of a transaction.
         */
        SUSPEND_EXISTING

    }

    public enum ExceptionResult {
        /**
         * The transaction should be committed.
         */
        COMMIT,
        /**
         * The transaction should be rolled back.
         */
        ROLLBACK
    }
}
