package io.quarkus.websockets.next.runtime;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Any;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.ContextInstanceHandle;
import io.quarkus.arc.CurrentContext;
import io.quarkus.arc.CurrentContextFactory;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.impl.ComputingCacheContextInstances;
import io.quarkus.arc.impl.ContextInstanceHandleImpl;
import io.quarkus.arc.impl.ContextInstances;
import io.quarkus.arc.impl.LazyValue;

public class WebSocketSessionContext implements ManagedContext {

    private static final Logger LOG = Logger.getLogger(WebSocketSessionContext.class);

    private final CurrentContext<SessionContextState> currentContext;
    private final LazyValue<Event<Object>> initializedEvent;
    private final LazyValue<Event<Object>> beforeDestroyEvent;
    private final LazyValue<Event<Object>> destroyEvent;

    public WebSocketSessionContext(CurrentContextFactory currentContextFactory) {
        this.currentContext = currentContextFactory.create(SessionScoped.class);
        this.initializedEvent = newEvent(Initialized.Literal.SESSION, Any.Literal.INSTANCE);
        this.beforeDestroyEvent = newEvent(BeforeDestroyed.Literal.SESSION, Any.Literal.INSTANCE);
        this.destroyEvent = newEvent(Destroyed.Literal.SESSION, Any.Literal.INSTANCE);
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return SessionScoped.class;
    }

    @Override
    public ContextState getState() {
        SessionContextState state = currentState();
        if (state == null) {
            throw notActive();
        }
        return state;
    }

    @Override
    public ContextState activate(ContextState initialState) {
        if (initialState == null) {
            SessionContextState state = initializeContextState();
            currentContext.set(state);
            return state;
        } else {
            if (initialState instanceof SessionContextState) {
                currentContext.set((SessionContextState) initialState);
                return initialState;
            } else {
                throw new IllegalArgumentException("Invalid initial state: " + initialState.getClass().getName());
            }
        }
    }

    @Override
    public void deactivate() {
        currentContext.remove();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        Objects.requireNonNull(contextual, "Contextual must not be null");
        Objects.requireNonNull(creationalContext, "CreationalContext must not be null");
        InjectableBean<T> bean = (InjectableBean<T>) contextual;
        if (!SessionScoped.class.getName().equals(bean.getScope().getName())) {
            throw invalidScope();
        }
        SessionContextState state = currentState();
        if (state == null || !state.isValid()) {
            throw notActive();
        }
        return (T) state.contextInstances.computeIfAbsent(bean.getIdentifier(), new Supplier<ContextInstanceHandle<?>>() {

            @Override
            public ContextInstanceHandle<?> get() {
                return new ContextInstanceHandleImpl<>(bean, contextual.create(creationalContext), creationalContext);
            }
        }).get();
    }

    @Override
    public <T> T get(Contextual<T> contextual) {
        Objects.requireNonNull(contextual, "Contextual must not be null");
        InjectableBean<T> bean = (InjectableBean<T>) contextual;
        if (!SessionScoped.class.getName().equals(bean.getScope().getName())) {
            throw invalidScope();
        }
        SessionContextState state = currentState();
        if (state == null || !state.isValid()) {
            throw notActive();
        }
        @SuppressWarnings("unchecked")
        ContextInstanceHandle<T> instance = (ContextInstanceHandle<T>) state.contextInstances
                .getIfPresent(bean.getIdentifier());
        return instance == null ? null : instance.get();
    }

    @Override
    public boolean isActive() {
        SessionContextState contextState = currentState();
        return contextState == null ? false : contextState.isValid();
    }

    @Override
    public void destroy() {
        destroy(currentState());
    }

    @Override
    public void destroy(Contextual<?> contextual) {
        SessionContextState state = currentState();
        if (state == null || !state.isValid()) {
            throw notActive();
        }
        InjectableBean<?> bean = (InjectableBean<?>) contextual;
        ContextInstanceHandle<?> instance = state.contextInstances.remove(bean.getIdentifier());
        if (instance != null) {
            instance.destroy();
        }
    }

    @Override
    public void destroy(ContextState state) {
        if (state == null) {
            // nothing to destroy
            return;
        }
        if (state instanceof SessionContextState) {
            SessionContextState sessionState = ((SessionContextState) state);
            if (sessionState.invalidate()) {
                fireIfNotNull(beforeDestroyEvent.get());
                sessionState.contextInstances.removeEach(ContextInstanceHandle::destroy);
                fireIfNotNull(destroyEvent.get());
            }
        } else {
            throw new IllegalArgumentException("Invalid state implementation: " + state.getClass().getName());
        }
    }

    SessionContextState initializeContextState() {
        SessionContextState state = new SessionContextState(new ComputingCacheContextInstances());
        fireIfNotNull(initializedEvent.get());
        return state;
    }

    private SessionContextState currentState() {
        return currentContext.get();
    }

    private IllegalArgumentException invalidScope() {
        throw new IllegalArgumentException("The bean does not declare @SessionScoped");
    }

    private ContextNotActiveException notActive() {
        return new ContextNotActiveException("Session context is not active");
    }

    private void fireIfNotNull(Event<Object> event) {
        if (event != null) {
            try {
                event.fire(toString());
            } catch (Exception e) {
                LOG.warn("An error occurred during delivery of the context lifecycle event for " + toString(), e);
            }
        }
    }

    private static LazyValue<Event<Object>> newEvent(Annotation... qualifiers) {
        return new LazyValue<>(new Supplier<Event<Object>>() {
            @Override
            public Event<Object> get() {
                ArcContainer container = Arc.container();
                if (container.resolveObserverMethods(Object.class, qualifiers).isEmpty()) {
                    return null;
                }
                return container.beanManager().getEvent().select(qualifiers);
            }
        });
    }

    static class SessionContextState implements ContextState {

        // Using 0 as default value enable removing an initialization
        // in the constructor, piggybacking on the default value.
        // As per https://docs.oracle.com/javase/specs/jls/se8/html/jls-12.html#jls-12.5
        // the default field values are set before 'this' is accessible, hence
        // they should be the very first value observable even in presence of
        // unsafe publication of this object.
        private static final int VALID = 0;
        private static final int INVALID = 1;
        private static final VarHandle IS_VALID;

        static {
            try {
                IS_VALID = MethodHandles.lookup().findVarHandle(SessionContextState.class, "isValid", int.class);
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
        }

        private final ContextInstances contextInstances;
        private volatile int isValid;

        SessionContextState(ContextInstances contextInstances) {
            this.contextInstances = contextInstances;
        }

        @Override
        public Map<InjectableBean<?>, Object> getContextualInstances() {
            return contextInstances.getAllPresent().stream()
                    .collect(Collectors.toUnmodifiableMap(ContextInstanceHandle::getBean, ContextInstanceHandle::get));
        }

        /**
         * @return {@code true} if the state was successfully invalidated, {@code false} otherwise
         */
        boolean invalidate() {
            // Atomically sets the value just like AtomicBoolean.compareAndSet(boolean, boolean)
            return IS_VALID.compareAndSet(this, VALID, INVALID);
        }

        @Override
        public boolean isValid() {
            return isValid == VALID;
        }

    }

}
