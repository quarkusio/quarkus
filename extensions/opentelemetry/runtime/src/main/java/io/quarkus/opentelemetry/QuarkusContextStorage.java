package io.quarkus.opentelemetry;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.Scope;
import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.ext.web.RoutingContext;

public enum QuarkusContextStorage implements ContextStorage {
    INSTANCE;

    private static final Logger log = Logger.getLogger(QuarkusContextStorage.class);
    private static final String CONTEXT_KEY = QuarkusContextStorage.class.getName() + ".activeContext";

    private static final ThreadLocal<RoutingContext> THREAD_LOCAL_ROUTING_CONTEXT = new ThreadLocal<>();

    @Override
    public Scope attach(Context toAttach) {
        if (toAttach == null) {
            // Not allowed
            return NoopScope.INSTANCE;
        }

        Context beforeAttach = current();
        if (toAttach == beforeAttach || beforeAttach == Context.root()) {
            return NoopScope.INSTANCE;
        }

        RoutingContext routingContext = getRoutingContext();
        if (routingContext != null) {
            routingContext.put(CONTEXT_KEY, toAttach);
        } else {
            log.debug("RoutingContext not available. Unable to set new Context onto it.");
        }

        return () -> {
            if (routingContext != null) {
                routingContext.put(CONTEXT_KEY, beforeAttach);
            }
        };
    }

    @Override
    public Context current() {
        RoutingContext routingContext = getRoutingContext();
        if (routingContext != null) {
            return routingContext.get(CONTEXT_KEY);
        }
        return null;
    }

    private RoutingContext getRoutingContext() {
        try {
            RoutingContext currentRoutingContext = CDI.current().select(CurrentVertxRequest.class).get().getCurrent();

            // We no longer need temporary storage of RoutingContext on the thread as CDI Request Scope is active
            THREAD_LOCAL_ROUTING_CONTEXT.remove();

            return currentRoutingContext;
        } catch (ContextNotActiveException cnae) {
            return THREAD_LOCAL_ROUTING_CONTEXT.get();
        }
    }

    public void setRoutingContext(RoutingContext internalRoutingContext) {
        THREAD_LOCAL_ROUTING_CONTEXT.set(internalRoutingContext);
    }

    public void clearRoutingContext(RoutingContext routingContext) {
        if (routingContext.equals(THREAD_LOCAL_ROUTING_CONTEXT.get())) {
            THREAD_LOCAL_ROUTING_CONTEXT.remove();
        }
    }

    enum NoopScope implements Scope {
        INSTANCE;

        @Override
        public void close() {
        }
    }
}
