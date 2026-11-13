package io.quarkus.narayana.jta.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configures the behavior of the {@code Transactional} interceptors in the cases where
 * Quarkus deliberately deviates from the Jakarta Transactions specification.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.transactional")
public interface TransactionalAnnotationConfiguration {

    /**
     * Whether a checked exception thrown by a {@code @Transactional} method marks the transaction for rollback.
     * <p>
     * Note that {@code rollbackOn} and {@code dontRollbackOn} on the {@code @Transactional} annotation,
     * and the {@link io.quarkus.transaction.annotations.Rollback} annotation on the exception class,
     * always take precedence. It never applies to unchecked exceptions, which always cause a rollback unless
     * {@code dontRollbackOn} or {@code @Rollback(false)} say otherwise.
     * <p>
     * The default is {@code rollback}, which deviates from the Jakarta Transactions specification
     * which only mandates a rollback for unchecked exceptions. Set this property to
     * {@code no-rollback} to restore the specification behavior and be warned every time a checked exception
     * escapes without a rollback, or to {@code no-rollback-quiet} to restore it silently.
     *
     * @since 4.0
     */
    @WithDefault("rollback")
    CheckedExceptionBehavior checkedExceptions();
}
