package io.quarkus.narayana.jta;

import java.util.function.Function;

/**
 * Enum that can be used to control the decision to rollback or commit based on the type of an exception.
 *
 * @see QuarkusTransaction#runner(TransactionSemantic)
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
