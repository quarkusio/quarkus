package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.RequestScoped;

import org.jboss.logging.Logger;

import io.quarkus.arc.CurrentContext;
import io.quarkus.arc.impl.EventImpl.Notifier;

/**
 * The built-in context for {@link RequestScoped}.
 *
 * @author Martin Kouba
 */
class RequestContext extends CurrentManagedContext {

    private static final Logger LOG = Logger.getLogger("io.quarkus.arc.requestContext");

    public RequestContext(CurrentContext<CurrentContextState> currentContext, Notifier<Object> initializedNotifier,
            Notifier<Object> beforeDestroyedNotifier, Notifier<Object> destroyedNotifier,
            Supplier<ContextInstances> contextInstances) {
        super(currentContext, contextInstances, initializedNotifier != null ? initializedNotifier::notify : null,
                beforeDestroyedNotifier != null ? beforeDestroyedNotifier::notify : null,
                destroyedNotifier != null ? destroyedNotifier::notify : null);
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return RequestScoped.class;
    }

    @Override
    protected Logger traceLog() {
        return LOG;
    }

    protected void traceActivate(ContextState initialState) {
        String stack = Arrays.stream(Thread.currentThread().getStackTrace())
                .skip(2)
                .limit(7)
                .map(se -> "\n\t" + se.toString())
                .collect(Collectors.joining());
        LOG.tracef("Activate %s %s\n\t...",
                initialState != null ? Integer.toHexString(initialState.hashCode()) : "new", stack);
    }

    protected void traceDeactivate() {
        String stack = Arrays.stream(Thread.currentThread().getStackTrace())
                .skip(2)
                .limit(7)
                .map(se -> "\n\t" + se.toString())
                .collect(Collectors.joining());
        LOG.tracef("Deactivate%s\n\t...", stack);
    }

    protected void traceDestroy(ContextState state) {
        String stack = Arrays.stream(Thread.currentThread().getStackTrace())
                .skip(2)
                .limit(7)
                .map(se -> "\n\t" + se.toString())
                .collect(Collectors.joining());
        LOG.tracef("Destroy %s%s\n\t...", state != null ? Integer.toHexString(state.hashCode()) : "", stack);
    }

    protected ContextNotActiveException notActive() {
        String msg = "Request context is not active - you can activate the request context for a specific method using the @ActivateRequestContext interceptor binding";
        return new ContextNotActiveException(msg);
    }

}
