package io.quarkus.arc.impl;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;

import org.jboss.logging.Logger;

import io.quarkus.arc.ContextInstanceHandle;
import io.quarkus.arc.CurrentContext;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.ManagedContext;

/**
 * A managed context backed by the {@link CurrentContext}.
 */
public abstract class CurrentManagedContext implements ManagedContext {

    private static final Logger LOG = Logger.getLogger(CurrentManagedContext.class);

    private final CurrentContext<CurrentContextState> currentContext;

    private final Supplier<ContextInstances> contextInstances;

    private final Consumer<Object> initializedNotifier;
    private final Consumer<Object> beforeDestroyedNotifier;
    private final Consumer<Object> destroyedNotifier;

    protected CurrentManagedContext(CurrentContext<CurrentContextState> currentContext,
            Supplier<ContextInstances> contextInstances, Consumer<Object> initializedNotifier,
            Consumer<Object> beforeDestroyedNotifier, Consumer<Object> destroyedNotifier) {
        this.currentContext = currentContext;
        this.contextInstances = contextInstances;
        this.initializedNotifier = initializedNotifier;
        this.beforeDestroyedNotifier = beforeDestroyedNotifier;
        this.destroyedNotifier = destroyedNotifier;
    }

    @Override
    public ContextState getState() {
        CurrentContextState state = currentState();
        if (state == null) {
            throw notActive();
        }
        return state;
    }

    @Override
    public ContextState activate(ContextState initialState) {
        if (traceLog().isTraceEnabled()) {
            traceActivate(initialState);
        }
        if (initialState == null) {
            CurrentContextState state = initializeState();
            currentContext.set(state);
            if (state.shouldFireInitializedEvent()) {
                fireIfNotNull(initializedNotifier);
            }
            return state;
        } else {
            if (initialState instanceof CurrentContextState current) {
                currentContext.set(current);
                if (current.isValid() && current.shouldFireInitializedEvent()) {
                    fireIfNotNull(initializedNotifier);
                }
                return initialState;
            } else {
                throw new IllegalArgumentException("Invalid initial state: " + initialState.getClass().getName());
            }
        }
    }

    @Override
    public void deactivate() {
        if (traceLog().isTraceEnabled()) {
            traceDeactivate();
        }
        currentContext.remove();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getIfActive(Contextual<T> contextual, Function<Contextual<T>, CreationalContext<T>> creationalContextFun) {
        Objects.requireNonNull(contextual, "Contextual must not be null");
        Objects.requireNonNull(creationalContextFun, "CreationalContext function must not be null");
        InjectableBean<T> bean = (InjectableBean<T>) contextual;
        if (!Scopes.scopeMatches(this, bean)) {
            throw Scopes.scopeDoesNotMatchException(this, bean);
        }
        CurrentContextState state = currentState();
        if (state == null || !state.isValid()) {
            return null;
        }
        ContextInstances contextInstances = state.contextInstances;
        ContextInstanceHandle<T> instance = (ContextInstanceHandle<T>) contextInstances.getIfPresent(bean.getIdentifier());
        if (instance == null) {
            CreationalContext<T> creationalContext = creationalContextFun.apply(contextual);
            return (T) contextInstances.computeIfAbsent(bean.getIdentifier(), new Supplier<ContextInstanceHandle<?>>() {

                @Override
                public ContextInstanceHandle<?> get() {
                    return new ContextInstanceHandleImpl<>(bean, contextual.create(creationalContext), creationalContext);
                }
            }).get();
        }
        return instance.get();
    }

    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        T result = getIfActive(contextual,
                CreationalContextImpl.unwrap(Objects.requireNonNull(creationalContext, "CreationalContext must not be null")));
        if (result == null) {
            throw notActive();
        }
        return result;
    }

    @Override
    public <T> T get(Contextual<T> contextual) {
        Objects.requireNonNull(contextual, "Contextual must not be null");
        InjectableBean<T> bean = (InjectableBean<T>) contextual;
        if (!Scopes.scopeMatches(this, bean)) {
            throw Scopes.scopeDoesNotMatchException(this, bean);
        }
        CurrentContextState state = currentState();
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
        CurrentContextState contextState = currentState();
        return contextState == null ? false : contextState.isValid();
    }

    @Override
    public void destroy() {
        destroy(currentState());
    }

    @Override
    public void destroy(Contextual<?> contextual) {
        CurrentContextState state = currentState();
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
        if (traceLog().isTraceEnabled()) {
            traceDestroy(state);
        }
        if (state == null) {
            // nothing to destroy
            return;
        }
        if (state instanceof CurrentContextState) {
            CurrentContextState currentState = ((CurrentContextState) state);
            if (currentState.isValid() && currentState.shouldFireBeforeDestroyedEvent()) {
                fireIfNotNull(beforeDestroyedNotifier);
            }
            if (currentState.invalidate()) {
                currentState.contextInstances.removeEach(new Consumer<>() {
                    @Override
                    public void accept(ContextInstanceHandle<?> contextInstanceHandle) {
                        contextInstanceHandle.destroy();
                    }
                });
                fireIfNotNull(destroyedNotifier);
            }
        } else {
            throw new IllegalArgumentException("Invalid state implementation: " + state.getClass().getName());
        }
    }

    @Override
    public CurrentContextState initializeState() {
        CurrentContextState state = new CurrentContextState(contextInstances.get());
        return state;
    }

    protected Logger traceLog() {
        return LOG;
    }

    protected void traceActivate(ContextState initialState) {
        // Noop
    }

    protected void traceDeactivate() {
        // Noop
    }

    protected void traceDestroy(ContextState state) {
        // Noop
    }

    private CurrentContextState currentState() {
        return currentContext.get();
    }

    protected abstract ContextNotActiveException notActive();

    private void fireIfNotNull(Consumer<Object> notifier) {
        if (notifier != null) {
            try {
                notifier.accept(toString());
            } catch (Exception e) {
                LOG.warn("An error occurred during delivery of the context lifecycle event for " + toString(), e);
            }
        }
    }

    public static class CurrentContextState implements ContextState {

        // Using 0 as default value enable removing an initialization
        // in the constructor, piggybacking on the default value.
        // As per https://docs.oracle.com/javase/specs/jls/se8/html/jls-12.html#jls-12.5
        // the default field values are set before 'this' is accessible, hence
        // they should be the very first value observable even in presence of
        // unsafe publication of this object.
        private static final int VALID = 0;
        private static final int INVALID = 1;
        private static final VarHandle IS_VALID;

        private static final int FALSE = 0;
        private static final int TRUE = 1;
        private static final VarHandle INITIALIZED_FIRED;
        private static final VarHandle BEFORE_DESTROYED_FIRED;

        static {
            try {
                IS_VALID = MethodHandles.lookup().findVarHandle(CurrentContextState.class, "isValid", int.class);
                INITIALIZED_FIRED = MethodHandles.lookup().findVarHandle(CurrentContextState.class, "initializedFired",
                        int.class);
                BEFORE_DESTROYED_FIRED = MethodHandles.lookup().findVarHandle(CurrentContextState.class, "beforeDestroyedFired",
                        int.class);
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
        }

        private final ContextInstances contextInstances;
        private volatile int isValid;
        private volatile int initializedFired;
        private volatile int beforeDestroyedFired;

        CurrentContextState(ContextInstances contextInstances) {
            this.contextInstances = Objects.requireNonNull(contextInstances);
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

        boolean shouldFireInitializedEvent() {
            return INITIALIZED_FIRED.compareAndSet(this, FALSE, TRUE);
        }

        boolean shouldFireBeforeDestroyedEvent() {
            return BEFORE_DESTROYED_FIRED.compareAndSet(this, FALSE, TRUE);
        }

    }

}
