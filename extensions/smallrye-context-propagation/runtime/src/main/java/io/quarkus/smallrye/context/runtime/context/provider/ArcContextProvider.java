package io.quarkus.smallrye.context.runtime.context.provider;

import java.util.Collection;
import java.util.Map;

import javax.enterprise.inject.spi.CDI;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.ContextInstanceHandle;
import io.quarkus.arc.ManagedContext;

/**
 * Context propagation for Arc
 * Only handles Request context as that's currently the only one in Arc that needs propagation.
 */
public class ArcContextProvider implements ThreadContextProvider {

    private static ThreadContextSnapshot NOOP_SNAPSHOT = () -> () -> {
    };

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> map) {
        if (!isCdiAvailable()) {
            //return null as per docs to state that propagation of this context is not supported
            return null;
        }

        ArcContainer arc = Arc.container();
        if (!isContextActiveOnThisThread(arc)) {
            // request context not active, nothing to propagate, return no-op
            return NOOP_SNAPSHOT;
        }

        // capture all instances
        Collection<ContextInstanceHandle<?>> instancesToPropagate = arc.requestContext().getAll();
        return () -> {
            ThreadContextController controller;
            ManagedContext requestContext = arc.requestContext();
            // this is executed on another thread, context can but doesn't need to be active here
            if (isContextActiveOnThisThread(arc)) {
                // context active, store current state, feed it new one and restore state afterwards
                Collection<ContextInstanceHandle<?>> instancesToRestore = requestContext.getAll();
                requestContext.deactivate();
                requestContext.activate(instancesToPropagate);
                controller = () -> {
                    // clean up, reactivate context with previous values
                    requestContext.deactivate();
                    requestContext.activate(instancesToRestore);
                };
            } else {
                // context not active, activate and pass it new instance, deactivate afterwards
                requestContext.activate(instancesToPropagate);
                controller = () -> {
                    requestContext.deactivate();
                };
            }
            return controller;
        };
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> map) {
        // note that by cleared we mean that we still activate context if need be, just leave the contents blank
        if (!isCdiAvailable()) {
            //return null as per docs to state that propagation of this context is not supported
            return null;
        }

        ArcContainer arc = Arc.container();
        if (!isContextActiveOnThisThread(arc)) {
            // request context not active, nothing to propagate, return no-op
            return NOOP_SNAPSHOT;
        }

        return () -> {
            ThreadContextController controller;
            ManagedContext requestContext = arc.requestContext();
            // this is executed on another thread, context can but doesn't need to be active here
            if (isContextActiveOnThisThread(arc)) {
                // context active, store current state, start blank context anew and restore state afterwards
                Collection<ContextInstanceHandle<?>> instancesToRestore = requestContext.getAll();
                requestContext.deactivate();
                requestContext.activate();
                controller = () -> {
                    // clean up, reactivate context with previous values
                    requestContext.deactivate();
                    requestContext.activate(instancesToRestore);
                };
            } else {
                // context not active, activate blank one, deactivate afterwards
                requestContext.activate();
                controller = () -> {
                    requestContext.deactivate();
                };
            }
            return controller;
        };
    }

    @Override
    public String getThreadContextType() {
        return ThreadContext.CDI;
    }

    /**
     * Checks if CDI is available within the application by using {@code CDI.current()}.
     * If an exception is thrown, it is suppressed and false is returns, otherwise true is returned.
     *
     * @return true if CDI can be used, false otherwise
     */
    private boolean isCdiAvailable() {
        try {
            return CDI.current() != null;
        } catch (IllegalStateException e) {
            // no CDI provider found, CDI isn't available
            return false;
        }
    }

    private boolean isContextActiveOnThisThread(ArcContainer arc) {
        return arc.requestContext().isActive();
    }
}
