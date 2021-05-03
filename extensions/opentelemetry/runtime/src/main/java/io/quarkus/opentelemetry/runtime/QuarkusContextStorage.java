package io.quarkus.opentelemetry.runtime;

import org.jboss.logging.Logger;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxInternal;

public enum QuarkusContextStorage implements ContextStorage {
    INSTANCE;

    private static final Logger log = Logger.getLogger(QuarkusContextStorage.class);

    private static final String CONTEXT_KEY = QuarkusContextStorage.class.getName() + ".activeContext";

    static VertxInternal vertx;

    @Override
    public Scope attach(Context toAttach) {
        if (toAttach == null) {
            // Not allowed
            return Scope.noop();
        }

        Context beforeAttach = current();
        if (toAttach == beforeAttach) {
            return Scope.noop();
        }

        io.vertx.core.Context vertxContext = getVertxContext();
        if (vertxContext != null) {
            vertxContext.put(CONTEXT_KEY, toAttach);
            return () -> {
                if (current() != toAttach) {
                    log.warn("Context in storage not the expected context, Scope.close was not called correctly");
                }
                if (beforeAttach == null) {
                    vertxContext.remove(CONTEXT_KEY);
                } else {
                    vertxContext.put(CONTEXT_KEY, beforeAttach);
                }
            };
        }

        return Scope.noop();
    }

    @Override
    public Context current() {
        io.vertx.core.Context vertxContext = getVertxContext();

        return vertxContext != null ? vertxContext.get(CONTEXT_KEY) : null;
    }

    private io.vertx.core.Context getVertxContext() {
        io.vertx.core.Context vertxContext = null;

        if (io.vertx.core.Context.isOnVertxThread() && Vertx.currentContext() != null) {
            vertxContext = Vertx.currentContext();
        } else {
            if (vertx != null) {
                vertxContext = vertx.getContext();
            }
        }
        return vertxContext;
    }
}
