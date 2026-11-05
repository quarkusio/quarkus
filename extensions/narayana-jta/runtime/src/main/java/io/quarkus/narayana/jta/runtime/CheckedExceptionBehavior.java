package io.quarkus.narayana.jta.runtime;

/**
 * An enum to control how the {@code @Transactional} interceptor handles checked exceptions when
 * neither {@code rollbackOn} nor {@code dontRollbackOn} match the exception and when the class of the
 * exception being handled is not annotated with {@link io.quarkus.transaction.annotations.Rollback}.
 *
 * @since 4.0
 */
public enum CheckedExceptionBehavior {
    /**
     * Mark the transaction for rollback similar to how unchecked exceptions behave.
     * This behaviour deviates from the Jakarta Transactions specification which only requires rollback for
     * unchecked exceptions.
     */
    ROLLBACK,

    /**
     * Leave the transaction untouched, as required by the Jakarta Transactions specification, and log a warning
     * every time a checked exception escapes a {@code @Transactional} method without causing a rollback.
     */
    NO_ROLLBACK,

    /**
     * Leave the transaction untouched, as required by the Jakarta Transactions specification, without logging
     * anything.
     */
    NO_ROLLBACK_QUIET
}
