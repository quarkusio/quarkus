package io.quarkus.observation.propagation;

import static io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle.setContextSafe;

import org.jboss.logging.Logger;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Vertx;

/**
 * Bridges the Micrometer Observation scope with the Vert.x Context.
 * When a Vert.x duplicated context is available, the scope is stored in
 * Vert.x local context data. Otherwise, falls back to Micrometer's ThreadLocal
 * via the application's ObservationRegistry.
 * Same pattern as QuarkusContextStorage for OpenTelemetry contexts.
 */
public final class ObservationContextStorage {

    private static final Logger logger = Logger.getLogger(ObservationContextStorage.class);
    private static final String OBSERVATION_SCOPE_KEY = ObservationContextStorage.class.getName() + ".observationScope";

    // FIXME Use low level storage with vert.x localcontext
    private static volatile ObservationRegistry registry;

    private ObservationContextStorage() {
    }

    public static void init(ObservationRegistry appRegistry) {
        registry = appRegistry;
    }

    public static Observation.Scope currentScope() {
        io.vertx.core.Context vertxContext = getVertxContext();
        if (vertxContext != null) {
            Observation.Scope scope = (Observation.Scope) VertxContext.localContextData(vertxContext)
                    .get(OBSERVATION_SCOPE_KEY);
            if (scope != null) {
                return scope;
            }
        }
        ObservationRegistry reg = registry;
        return reg != null ? reg.getCurrentObservationScope() : null;
    }

    public static Observation.Scope setCurrentScope(Observation.Scope scope) {
        Observation.Scope previous = currentScope();

        io.vertx.core.Context vertxContext = getVertxContext();
        if (vertxContext != null) {
            if (scope == null) {
                VertxContext.localContextData(vertxContext).remove(OBSERVATION_SCOPE_KEY);
            } else {
                VertxContext.localContextData(vertxContext).put(OBSERVATION_SCOPE_KEY, scope);
            }
        } else {
            ObservationRegistry reg = registry;
            if (reg != null) {
                reg.setCurrentObservationScope(scope);
            }
        }
        return previous;
    }

    private static io.vertx.core.Context getVertxContext() {
        io.vertx.core.Context context = Vertx.currentContext();
        if (context != null && VertxContext.isOnDuplicatedContext()) {
            return context;
        } else if (context != null) {
            logger.infov("Vert.x context without duplicated context. Creating a new one.");
            io.vertx.core.Context dc = VertxContext.createNewDuplicatedContext(context);
            // FIXME not sure if safe
            setContextSafe(dc, true);
            return dc;
        }
        return null;
    }
}
