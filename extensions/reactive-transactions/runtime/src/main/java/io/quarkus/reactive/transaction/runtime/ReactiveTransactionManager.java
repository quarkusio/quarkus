package io.quarkus.reactive.transaction.runtime;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import io.smallrye.common.vertx.ContextLocals;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

/**
 * Manages reactive transaction state via Vert.x ContextLocals,
 * analogous to {@code jakarta.transaction.TransactionManager} in JTA.
 * <p>
 * Only a single {@link ReactiveTransactionResource} can be enlisted per transaction (no XA support).
 * {@link ReactiveTransactionSynchronization} instances are registered programmatically per transaction.
 */
@ApplicationScoped
public class ReactiveTransactionManager {

    private static final Logger LOG = Logger.getLogger(ReactiveTransactionManager.class);

    public static final String TRANSACTIONAL_METHOD_KEY = "reactive.transaction.active";
    private static final String RESOURCE_KEY = "reactive.transaction.resource";
    private static final String SYNCHRONIZATIONS_KEY = "reactive.transaction.synchronizations";

    public boolean isActive() {
        Object value = ContextLocals.get(TRANSACTIONAL_METHOD_KEY, null);
        return value != null && (boolean) value;
    }

    public void begin() {
        LOG.tracef("Beginning reactive transaction");
        ContextLocals.put(TRANSACTIONAL_METHOD_KEY, true);
    }

    public void enlistResource(ReactiveTransactionResource resource) {
        ReactiveTransactionResource existing = ContextLocals.get(RESOURCE_KEY, null);
        if (existing != null) {
            throw new IllegalStateException(
                    "Cannot enlist multiple resources in a reactive transaction (XA not supported). "
                            + "Use a single datasource per @Transactional method, "
                            + "or use pool.withTransaction() for manual transaction management.");
        }
        LOG.tracef("Enlisting resource: %s", resource);
        ContextLocals.put(RESOURCE_KEY, resource);
    }

    public ReactiveTransactionResource getResource() {
        return ContextLocals.get(RESOURCE_KEY, null);
    }

    public void registerSynchronization(ReactiveTransactionSynchronization sync) {
        List<ReactiveTransactionSynchronization> syncs = ContextLocals.get(SYNCHRONIZATIONS_KEY, null);
        if (syncs == null) {
            syncs = new ArrayList<>();
            ContextLocals.put(SYNCHRONIZATIONS_KEY, syncs);
        }
        LOG.tracef("Registering synchronization: %s", sync);
        syncs.add(sync);
    }

    public Uni<Void> commit() {
        Context context = Vertx.currentContext();
        List<ReactiveTransactionSynchronization> syncs = getSynchronizations();
        ReactiveTransactionResource resource = getResource();

        Uni<Void> chain = Uni.createFrom().voidItem();

        // beforeCommit on all synchronizations
        for (ReactiveTransactionSynchronization sync : syncs) {
            chain = chain.chain(() -> sync.beforeCommit(context)).replaceWithVoid();
        }

        // commit the resource
        if (resource != null) {
            chain = chain.chain(() -> toUni(resource.commit()))
                    .invoke(() -> LOG.tracef("Resource committed"));
        }

        // afterCommit on all synchronizations
        for (ReactiveTransactionSynchronization sync : syncs) {
            chain = chain.chain(() -> sync.afterCommit(context)).replaceWithVoid();
        }

        return chain;
    }

    public Uni<Void> rollback() {
        Context context = Vertx.currentContext();
        List<ReactiveTransactionSynchronization> syncs = getSynchronizations();
        ReactiveTransactionResource resource = getResource();

        Uni<Void> chain = Uni.createFrom().voidItem();

        // rollback the resource
        if (resource != null) {
            chain = chain.chain(() -> toUni(resource.rollback()))
                    .invoke(() -> LOG.tracef("Resource rolled back"));
        }

        // afterCommit on all synchronizations (called after rollback too, like JTA)
        for (ReactiveTransactionSynchronization sync : syncs) {
            chain = chain.chain(() -> sync.afterCommit(context)).replaceWithVoid();
        }

        return chain;
    }

    public Uni<Void> close() {
        ReactiveTransactionResource resource = getResource();

        Uni<Void> chain = Uni.createFrom().voidItem();

        if (resource != null) {
            chain = chain.chain(() -> toUni(resource.close()))
                    .invoke(() -> LOG.tracef("Resource closed"));
        }

        return chain.eventually(this::cleanup);
    }

    private void cleanup() {
        ContextLocals.remove(TRANSACTIONAL_METHOD_KEY);
        ContextLocals.remove(RESOURCE_KEY);
        ContextLocals.remove(SYNCHRONIZATIONS_KEY);
    }

    private List<ReactiveTransactionSynchronization> getSynchronizations() {
        List<ReactiveTransactionSynchronization> syncs = ContextLocals.get(SYNCHRONIZATIONS_KEY, null);
        return syncs != null ? syncs : List.of();
    }

    private static <T> Uni<T> toUni(Future<? extends T> future) {
        return Uni.createFrom()
                .emitter(emitter -> future.onComplete(emitter::complete, emitter::fail));
    }
}
