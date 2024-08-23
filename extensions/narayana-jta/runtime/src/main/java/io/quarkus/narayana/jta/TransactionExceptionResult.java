package io.quarkus.narayana.jta;

import java.util.function.Function;

/**
 * Enum that can be used to control the decision to rollback or commit based on the type of an exception.
 *
 * @see QuarkusTransaction#joiningExisting()
 * @see QuarkusTransaction#requiringNew()
 * @see QuarkusTransaction#disallowingExisting()
 * @see QuarkusTransaction#suspendingExisting()
 * @see QuarkusTransaction#runner(TransactionSemantics)
 * @see TransactionRunnerOptions#exceptionHandler(Function)
 */
public enum TransactionExceptionResult {

    /**
     * The transaction should be committed.
     */
    COMMIT,
    /**
     * The transaction should be rolled back.
     */
    ROLLBACK

}
