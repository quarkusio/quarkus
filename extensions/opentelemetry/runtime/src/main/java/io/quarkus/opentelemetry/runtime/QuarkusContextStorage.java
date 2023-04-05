package io.quarkus.opentelemetry.runtime;

import static io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle.setContextSafe;
import static io.smallrye.common.vertx.VertxContext.isDuplicatedContext;

import org.jboss.logging.Logger;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Vertx;

/**
 * Bridges the OpenTelemetry ContextStorage with the Vert.x Context. The default OpenTelemetry ContextStorage (based in
 * ThreadLocals) is not suitable for Vert.x. In this case, the OpenTelemetry Context piggybacks on top of the Vert.x
 * Context. If the Vert.x Context is not available, fallbacks to an MDC enabled context storage that wraps the default
 * OpenTelemetry ContextStorage.
 */
public enum QuarkusContextStorage implements ContextStorage {
    INSTANCE;

    private static final Logger log = Logger.getLogger(QuarkusContextStorage.class);
    private static final String OTEL_CONTEXT = QuarkusContextStorage.class.getName() + ".otelContext";

    private static final ContextStorage FALLBACK_CONTEXT_STORAGE = MDCEnabledContextStorage.INSTANCE;
    static Vertx vertx;

    /**
     * Attach the OpenTelemetry Context to the current Context. If a Vert.x Context is available, and it is a duplicated
     * Vert.x Context the OpenTelemetry Context is attached to the Vert.x Context. Otherwise, fallback to the
     * OpenTelemetry default ContextStorage.
     *
     * @param toAttach the OpenTelemetry Context to attach
     * @return the Scope of the OpenTelemetry Context
     */
    @Override
    public Scope attach(Context toAttach) {
        io.vertx.core.Context vertxContext = getVertxContext();
        return vertxContext != null && isDuplicatedContext(vertxContext) ? attach(vertxContext, toAttach)
                : FALLBACK_CONTEXT_STORAGE.attach(toAttach);
    }

    /**
     * Attach the OpenTelemetry Context in the Vert.x Context if it is a duplicated Vert.x Context.
     *
     * @param vertxContext the Vert.x Context to attach the OpenTelemetry Context
     * @param toAttach the OpenTelemetry Context to attach
     * @return the Scope of the OpenTelemetry Context
     */
    public Scope attach(io.vertx.core.Context vertxContext, Context toAttach) {
        if (vertxContext == null || toAttach == null) {
            return Scope.noop();
        }

        // We don't allow to attach the OpenTelemetry Context to a Vert.x Context that is not a duplicate.
        if (!isDuplicatedContext(vertxContext)) {
            throw new IllegalArgumentException(
                    "The Vert.x Context to attach the OpenTelemetry Context must be a duplicated Context");
        }

        Context beforeAttach = getContext(vertxContext);
        if (toAttach == beforeAttach) {
            return Scope.noop();
        }
        vertxContext.putLocal(OTEL_CONTEXT, toAttach);
        OpenTelemetryUtil.setMDCData(toAttach, vertxContext);
        //        System.out.println(Thread.currentThread().getName() + " UPDATE from " +
        //                Span.fromContext(beforeAttach).getSpanContext().getSpanId()
        //                + " to " + Span.fromContext(toAttach).getSpanContext().getSpanId());
        //        new Exception().printStackTrace();

        return new Scope() {

            @Override
            public void close() {
                //                System.out.println(Thread.currentThread().getName() + " REVERT from actual " +
                //                        Span.fromContext(getContext(vertxContext)).getSpanContext().getSpanId() + ", expected "
                //                        + Span.fromContext(toAttach).getSpanContext().getSpanId()
                //                        + ", to " + Span.fromContext(beforeAttach).getSpanContext().getSpanId());
                if (getContext(vertxContext) != toAttach) {
                    log.warn("Context in storage not the expected context, Scope.close was not called correctly");
                }

                if (beforeAttach == null) {
                    vertxContext.removeLocal(OTEL_CONTEXT);
                    OpenTelemetryUtil.clearMDCData(vertxContext);
                } else {
                    vertxContext.putLocal(OTEL_CONTEXT, beforeAttach);
                    OpenTelemetryUtil.setMDCData(beforeAttach, vertxContext);
                }
            }
        };
    }

    /**
     * Gets the current OpenTelemetry Context from the current Vert.x Context if one exists or from the default
     * ContextStorage. The current Vert.x Context must be a duplicated Context.
     *
     * @return the current OpenTelemetry Context or null.
     */
    @Override
    public Context current() {
        io.vertx.core.Context current = getVertxContext();
        if (current != null) {
            return current.getLocal(OTEL_CONTEXT);
        } else {
            return FALLBACK_CONTEXT_STORAGE.current();
        }
    }

    /**
     * Gets the OpenTelemetry Context in a Vert.x Context. The Vert.x Context has to be a duplicate context.
     *
     * @param vertxContext a Vert.x Context.
     * @return the OpenTelemetry Context if exists in the Vert.x Context or null.
     */
    public static Context getContext(io.vertx.core.Context vertxContext) {
        return vertxContext != null && isDuplicatedContext(vertxContext) ? vertxContext.getLocal(OTEL_CONTEXT) : null;
    }

    /**
     * Gets the current duplicated context or a new duplicated context if a Vert.x Context exists. Multiple invocations
     * of this method may return the same or different context. If the current context is a duplicate one, multiple
     * invocations always return the same context. If the current context is not duplicated, a new instance is returned
     * with each method invocation.
     *
     * @return a duplicated Vert.x Context or null.
     */
    public static io.vertx.core.Context getVertxContext() {
        io.vertx.core.Context context = Vertx.currentContext();
        if (context != null && VertxContext.isOnDuplicatedContext()) {
            return context;
        } else if (context != null) {
            io.vertx.core.Context dc = VertxContext.createNewDuplicatedContext(context);
            setContextSafe(dc, true);
            return dc;
        }
        return null;
    }
}
