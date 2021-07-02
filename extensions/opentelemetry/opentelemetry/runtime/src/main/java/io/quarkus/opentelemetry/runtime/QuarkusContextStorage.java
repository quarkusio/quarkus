package io.quarkus.opentelemetry.runtime;

import org.jboss.logging.Logger;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;
import io.vertx.core.Vertx;

public enum QuarkusContextStorage implements ContextStorage {
    INSTANCE;

    private static final Logger log = Logger.getLogger(QuarkusContextStorage.class);

    public static final String ACTIVE_CONTEXT = QuarkusContextStorage.class.getName() + ".activeContext";

    static Vertx vertx;

    @Override
    public Scope attach(Context toAttach) {
        return attach(getVertxContext(), toAttach);
    }

    public Scope attach(io.vertx.core.Context vertxContext, Context toAttach) {
        if (toAttach == null) {
            // Not allowed
            return Scope.noop();
        }

        Context beforeAttach = getContext(vertxContext);
        if (toAttach == beforeAttach) {
            return Scope.noop();
        }

        if (vertxContext != null) {
            vertxContext.putLocal(ACTIVE_CONTEXT, toAttach);
            return () -> {
                if (getContext(vertxContext) != toAttach) {
                    log.warn("Context in storage not the expected context, Scope.close was not called correctly");
                }
                if (beforeAttach == null) {
                    vertxContext.removeLocal(ACTIVE_CONTEXT);
                } else {
                    vertxContext.putLocal(ACTIVE_CONTEXT, beforeAttach);
                }
            };
        }

        return Scope.noop();
    }

    @Override
    public Context current() {
        return getContext(getVertxContext());
    }

    private Context getContext(io.vertx.core.Context vertxContext) {
        return vertxContext != null ? vertxContext.getLocal(ACTIVE_CONTEXT) : null;
    }

    private io.vertx.core.Context getVertxContext() {
        return vertx.getOrCreateContext();
    }
}
