package io.quarkus.smallrye.reactivemessaging.runtime;

import java.util.concurrent.CompletionStage;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableContext.ContextState;
import io.quarkus.arc.ManagedContext;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.Invoker;

/**
 * Activates a CDI request context around {@link Invoker} calls for {@code @Incoming} methods.
 */
public final class IncomingRequestContextInvokerSupport {

    private IncomingRequestContextInvokerSupport() {
    }

    public static Object invoke(Invoker delegate, Object[] args) {
        ManagedContext requestContext = Arc.container().requestContext();
        if (requestContext.isActive()) {
            return delegate.invoke(args);
        }

        requestContext.activate();
        ContextState state = requestContext.getState();
        try {
            Object result = delegate.invoke(args);
            return wrapResult(result, requestContext, state);
        } catch (Throwable failure) {
            destroyAndDeactivate(requestContext, state);
            throw failure;
        }
    }

    private static Object wrapResult(Object result, ManagedContext requestContext, ContextState state) {
        if (result instanceof Uni<?> uni) {
            return uni.eventually(() -> destroyAndDeactivate(requestContext, state));
        }
        if (result instanceof Multi<?> multi) {
            return multi.onTermination().invoke(() -> destroyAndDeactivate(requestContext, state));
        }
        if (result instanceof CompletionStage<?> stage) {
            return stage.whenComplete((ignored, failure) -> destroyAndDeactivate(requestContext, state));
        }
        destroyAndDeactivate(requestContext, state);
        return result;
    }

    private static void destroyAndDeactivate(ManagedContext requestContext, ContextState state) {
        requestContext.destroy(state);
        requestContext.deactivate();
    }
}
