package io.quarkus.reactive.transaction.runtime;

import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;

/**
 * Lifecycle hooks for a reactive transaction, analogous to {@code jakarta.transaction.Synchronization} in JTA.
 * <p>
 * Instances are registered programmatically via
 * {@link ReactiveTransactionManager#registerSynchronization(ReactiveTransactionSynchronization)},
 * not through CDI.
 */
public interface ReactiveTransactionSynchronization {

    default Uni<?> beforeCommit(Context context) {
        return Uni.createFrom().voidItem();
    }

    default Uni<?> afterCommit(Context context) {
        return Uni.createFrom().voidItem();
    }
}
