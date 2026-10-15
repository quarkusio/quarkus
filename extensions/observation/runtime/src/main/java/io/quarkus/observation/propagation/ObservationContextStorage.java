package io.quarkus.observation.propagation;

import org.jboss.logging.Logger;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Vertx;

/**
 * Bridges the Micrometer Observation scope with the Vert.x Context.
 * <p>
 * When the current thread runs on a Vert.x duplicated context, the scope is stored in the duplicated context
 * local data so it survives dispatch to other threads within the same request. Otherwise, it falls back to the
 * ThreadLocal natively used by the Observation API.
 * <p>
 * This is the backing store used by {@link QuarkusObservationRegistry} and by the MicroProfile Context
 * Propagation provider. Same pattern as {@code QuarkusContextStorage} for OpenTelemetry contexts: the duplicated
 * context is the primary store and a ThreadLocal-based store is the fallback.
 */
public final class ObservationContextStorage {

    public static final String OBSERVATION_SCOPE_KEY = ObservationContextStorage.class.getName() + ".observationScope";
    private static final Logger log = Logger.getLogger(ObservationContextStorage.class);

    /**
     * Fallback store used when the current thread is not running on a Vert.x duplicated context.
     * <p>
     * This is intentionally a {@link ObservationRegistry#create() SimpleObservationRegistry}: its
     * {@code localObservationScope} is a JVM-wide {@code static} ThreadLocal shared by every
     * SimpleObservationRegistry instance. So this field is not a second scope store — it is a handle onto the
     * exact ThreadLocal the stock Observation API uses, which also lets us observe scopes opened through a
     * registry that does not delegate to this storage (e.g. a TestObservationRegistry).
     */
    private static final ObservationRegistry FALLBACK = ObservationRegistry.create();

    private ObservationContextStorage() {
    }

    public static Observation.Scope currentScope() {
        final io.vertx.core.Context duplicatedContext = currentDuplicatedContext();
        if (duplicatedContext != null) {
            final Observation.Scope scope = (Observation.Scope) VertxContext.localContextData(duplicatedContext)
                    .get(OBSERVATION_SCOPE_KEY);
            if (scope != null) {
                return scope;
            }
            // On a duplicated context but nothing stored there yet: the scope may have been opened through a
            // registry that does not delegate to this storage. Fall back to the Observation API ThreadLocal.
        }
        final Observation.Scope scope = FALLBACK.getCurrentObservationScope();
        if (scope != null && log.isDebugEnabled()) {
            log.debugv("Restore scope from ThreadLocal: {0}", scope);
        }
        return scope;
    }

    public static Observation.Scope setCurrentScope(Observation.Scope scope) {
        Observation.Scope previous = currentScope();

        io.vertx.core.Context duplicatedContext = currentDuplicatedContext();
        if (duplicatedContext != null) {
            if (scope == null) {
                final Object remove = VertxContext.localContextData(duplicatedContext).remove(OBSERVATION_SCOPE_KEY);
                if (log.isDebugEnabled()) {
                    log.debugv("Scope removed from DuplicatedContext: {0}", remove);
                }
            } else {
                VertxContext.localContextData(duplicatedContext).put(OBSERVATION_SCOPE_KEY, scope);
                if (log.isDebugEnabled()) {
                    log.debugv("Scope added to DuplicatedContext: {0}", scope);
                }
            }
        } else {
            // Not on a duplicated context: use the ThreadLocal natively used by the Observation API.
            if (log.isDebugEnabled()) {
                log.debugv("Store in ThreadLocal: {0}", scope);
            }
            FALLBACK.setCurrentObservationScope(scope);
        }
        return previous;
    }

    /**
     * Returns the current Vert.x context only when it is a duplicated context, which is the only
     * context we can safely use as a request-scoped store for the observation scope.
     * <p>
     * When the current thread is not on a duplicated context we must NOT create a throw-away
     * duplicated context: it would never be dispatched, so the scope stored in it would be lost on
     * the next lookup. In that case we return {@code null} so callers fall back to the ThreadLocal.
     *
     * @return the current duplicated Vert.x context, or {@code null} if the current thread is not
     *         running on a duplicated context.
     */
    private static io.vertx.core.Context currentDuplicatedContext() {
        io.vertx.core.Context context = Vertx.currentContext();
        if (context == null) {
            return null;
        }
        if (VertxContext.isOnDuplicatedContext()) {
            return context;
        }
        // On a Vert.x context that is not duplicated (e.g. the root event loop context).
        // Fall back to the Observation API ThreadLocal instead of losing the scope.
        log.debug("Not on a Vert.x duplicated context; using the Observation API ThreadLocal for the scope.");
        return null;
    }
}
