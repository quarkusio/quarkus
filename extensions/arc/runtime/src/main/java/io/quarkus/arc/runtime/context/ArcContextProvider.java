package io.quarkus.arc.runtime.context;

import java.util.Map;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.InjectableContext.ContextState;
import io.quarkus.arc.ManagedContext;

/**
 * Context propagation for ArC.
 * <p>
 * Only handles the request context as that's currently the only one in ArC that needs propagation.
 */
public class ArcContextProvider implements ThreadContextProvider {

    private static final ThreadContextController NOOP_CONTROLLER = new ThreadContextController() {
        @Override
        public void endContext() throws IllegalStateException {
        }
    };

    private static final ThreadContextSnapshot NULL_CONTEXT_SNAPSHOT = new NullContextSnapshot();
    private static final ThreadContextSnapshot CLEAR_CONTEXT_SNAPSHOT = new ClearContextSnapshot();

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> map) {
        ArcContainer container = Arc.container();
        if (container == null) {
            // return null as per docs to state that propagation of this context is not supported
            return null;
        }

        // capture the state, null indicates no active context while capturing snapshot
        InjectableContext.ContextState state = container.requestContext().getStateIfActive();
        if (state == null) {
            return NULL_CONTEXT_SNAPSHOT;
        } else {
            return new ContextSnapshot(state);
        }
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> map) {
        // note that by cleared we mean that we still activate context if need be, just leave the contents blank
        ArcContainer container = Arc.container();
        if (container == null) {
            // return null as per docs to state that propagation of this context is not supported
            return null;
        }
        return CLEAR_CONTEXT_SNAPSHOT;
    }

    @Override
    public String getThreadContextType() {
        return ThreadContext.CDI;
    }

    private static final class ClearContextSnapshot implements ThreadContextSnapshot {

        @Override
        public ThreadContextController begin() {
            // can be called later on, we should retrieve the container again
            ArcContainer container = Arc.container();
            if (container == null) {
                return NOOP_CONTROLLER;
            }

            ManagedContext requestContext = container.requestContext();
            InjectableContext.ContextState toRestore = requestContext.getStateIfActive();
            // this is executed on another thread, context can but doesn't need to be active here
            if (toRestore != null) {
                // context active, store current state, start blank context anew and restore state afterwards
                // it is not necessary to deactivate the context first - just overwrite the previous state
                requestContext.activate();
                return new RestoreContextController(requestContext, toRestore, true);
            } else {
                // context not active, activate blank one, terminate afterwards
                requestContext.activate();
                return requestContext::terminate;
            }
        }
    }

    private static final class NullContextSnapshot implements ThreadContextSnapshot {

        @Override
        public ThreadContextController begin() {
            // can be called later on, we should retrieve the container again
            ArcContainer container = Arc.container();
            if (container == null) {
                // this happens on shutdown, if we blow up here it can break shutdown, and stop
                // resources from being cleaned up, causing tests to fail
                return NOOP_CONTROLLER;
            }
            ManagedContext requestContext = container.requestContext();
            InjectableContext.ContextState toRestore = requestContext.getStateIfActive();
            // this is executed on another thread, context can but doesn't need to be active here
            if (toRestore != null) {
                // context active, store current state, feed it new one and restore state afterwards
                requestContext.deactivate();
                return new ThreadContextController() {
                    @Override
                    public void endContext() throws IllegalStateException {
                        requestContext.activate(toRestore.isValid() ? toRestore : null);
                    }
                };
            } else {
                // context not active
                return NOOP_CONTROLLER;
            }
        }
    }

    private static final class ContextSnapshot implements ThreadContextSnapshot {

        private final InjectableContext.ContextState state;

        public ContextSnapshot(ContextState state) {
            this.state = state;
        }

        @Override
        public ThreadContextController begin() {
            // can be called later on, we should retrieve the container again
            ArcContainer container = Arc.container();
            if (container == null) {
                // this happens on shutdown, if we blow up here it can break shutdown, and stop
                // resources from being cleaned up, causing tests to fail
                return NOOP_CONTROLLER;
            }
            ManagedContext requestContext = container.requestContext();
            InjectableContext.ContextState toRestore = requestContext.getStateIfActive();
            // this is executed on another thread, context can but doesn't need to be active here
            if (toRestore != null) {
                // context active, store current state, feed it new one and restore state afterwards
                // it is not necessary to deactivate the context first - just overwrite the previous state
                // if the context state is invalid (i.e. the context was destroyed by Arc), we instead create new state
                requestContext.activate(state.isValid() ? state : null);
                return new RestoreContextController(requestContext, toRestore);
            } else {
                // context not active, activate and pass it new instance, deactivate afterwards
                // if the context state is invalid (i.e. the context was destroyed by Arc), we instead create new state
                requestContext.activate(state.isValid() ? state : null);
                return requestContext::deactivate;
            }
        }

    }

    private static final class RestoreContextController implements ThreadContextController {

        private final ManagedContext requestContext;
        private final InjectableContext.ContextState stateToRestore;
        private final boolean destroyRequestContext;

        RestoreContextController(ManagedContext requestContext, ContextState stateToRestore) {
            this(requestContext, stateToRestore, false);
        }

        // in case of ClearContextSnapshot, we want to destroy instances of the intermediate context
        RestoreContextController(ManagedContext requestContext, ContextState stateToRestore,
                boolean destroyRequestContext) {
            this.requestContext = requestContext;
            this.stateToRestore = stateToRestore;
            this.destroyRequestContext = destroyRequestContext;
        }

        @Override
        public void endContext() throws IllegalStateException {
            if (destroyRequestContext) {
                requestContext.destroy();
            }
            // it is not necessary to deactivate the context first - just overwrite the previous state
            requestContext.activate(stateToRestore.isValid() ? stateToRestore : null);
        }

    }
}
