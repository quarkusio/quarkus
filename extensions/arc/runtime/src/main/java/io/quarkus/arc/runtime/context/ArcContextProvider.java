package io.quarkus.arc.runtime.context;

import java.util.Collection;
import java.util.Map;

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
        ArcContainer arc = Arc.container();
        if (arc == null || !arc.isRunning()) {
            //return null as per docs to state that propagation of this context is not supported
            return null;
        }

        if (!isContextActiveOnThisThread(arc)) {
            // request context not active, nothing to propagate, return no-op
            return NOOP_SNAPSHOT;
        }

        // capture all instances
        Collection<ContextInstanceHandle<?>> instancesToPropagate = arc.requestContext().getAll();
        return () -> {
            // can be called later on, we should retrieve the container again
            ArcContainer arcContainer = Arc.container();
            if (arcContainer == null || !arcContainer.isRunning()) {
                throw new IllegalStateException("Arc context propagation was attempted but the container is not running.");
            }
            ThreadContextController controller;
            ManagedContext requestContext = arcContainer.requestContext();
            // this is executed on another thread, context can but doesn't need to be active here
            if (isContextActiveOnThisThread(arcContainer)) {
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
        ArcContainer arc = Arc.container();
        if (arc == null || !arc.isRunning()) {
            //return null as per docs to state that propagation of this context is not supported
            return null;
        }

        if (!isContextActiveOnThisThread(arc)) {
            // request context not active, nothing to propagate, return no-op
            return NOOP_SNAPSHOT;
        }

        return () -> {
            // can be called later on, we should retrieve the container again
            ArcContainer arcContainer = Arc.container();
            if (arcContainer == null || !arcContainer.isRunning()) {
                throw new IllegalStateException("Arc context propagation was attempted but the container is not running.");
            }
            ThreadContextController controller;
            ManagedContext requestContext = arcContainer.requestContext();
            // this is executed on another thread, context can but doesn't need to be active here
            if (isContextActiveOnThisThread(arcContainer)) {
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

    private boolean isContextActiveOnThisThread(ArcContainer arc) {
        return arc.requestContext().isActive();
    }
}
