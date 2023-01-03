package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

import org.jboss.logging.Logger;

import io.quarkus.arc.ContextInstanceHandle;
import io.quarkus.arc.CurrentContext;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.impl.EventImpl.Notifier;

/**
 * The built-in context for {@link RequestScoped}.
 *
 * @author Martin Kouba
 */
class RequestContext implements ManagedContext {

    private static final Logger LOG = Logger.getLogger("io.quarkus.arc.requestContext");

    private final CurrentContext<RequestContextState> currentContext;

    private final Notifier<Object> initializedNotifier;
    private final Notifier<Object> beforeDestroyedNotifier;
    private final Notifier<Object> destroyedNotifier;

    public RequestContext(CurrentContext<RequestContextState> currentContext, Notifier<Object> initializedNotifier,
            Notifier<Object> beforeDestroyedNotifier, Notifier<Object> destroyedNotifier) {
        this.currentContext = currentContext;
        this.initializedNotifier = initializedNotifier;
        this.beforeDestroyedNotifier = beforeDestroyedNotifier;
        this.destroyedNotifier = destroyedNotifier;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return RequestScoped.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getIfActive(Contextual<T> contextual, Function<Contextual<T>, CreationalContext<T>> creationalContextFun) {
        Objects.requireNonNull(contextual, "Contextual must not be null");
        Objects.requireNonNull(creationalContextFun, "CreationalContext supplier must not be null");
        InjectableBean<T> bean = (InjectableBean<T>) contextual;
        if (!Scopes.scopeMatches(this, bean)) {
            throw Scopes.scopeDoesNotMatchException(this, bean);
        }
        RequestContextState ctxState = currentContext.get();
        if (ctxState == null) {
            // Context is not active!
            return null;
        }
        ContextInstanceHandle<T> instance = (ContextInstanceHandle<T>) ctxState.get(contextual);
        if (instance == null) {
            CreationalContext<T> creationalContext = creationalContextFun.apply(contextual);
            // Bean instance does not exist - create one if we have CreationalContext
            instance = new ContextInstanceHandleImpl<T>((InjectableBean<T>) contextual,
                    contextual.create(creationalContext), creationalContext);
            ctxState.put(contextual, instance);
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

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Contextual<T> contextual) {
        Objects.requireNonNull(contextual, "Contextual must not be null");
        InjectableBean<T> bean = (InjectableBean<T>) contextual;
        if (!Scopes.scopeMatches(this, bean)) {
            throw Scopes.scopeDoesNotMatchException(this, bean);
        }
        RequestContextState state = currentContext.get();
        if (state == null) {
            throw notActive();
        }
        ContextInstanceHandle<T> instance = (ContextInstanceHandle<T>) state.get(contextual);
        return instance == null ? null : instance.get();
    }

    @Override
    public boolean isActive() {
        return currentContext.get() != null;
    }

    @Override
    public void destroy(Contextual<?> contextual) {
        RequestContextState state = currentContext.get();
        if (state == null) {
            // Context is not active
            throw notActive();
        }
        ContextInstanceHandle<?> instance = state.remove(contextual);
        if (instance != null) {
            instance.destroy();
        }
    }

    @Override
    public void activate(ContextState initialState) {
        if (LOG.isTraceEnabled()) {
            String stack = Arrays.stream(Thread.currentThread().getStackTrace())
                    .skip(2)
                    .limit(7)
                    .map(se -> "\n\t" + se.toString())
                    .collect(Collectors.joining());
            LOG.tracef("Activate %s %s\n\t...",
                    initialState != null ? Integer.toHexString(initialState.hashCode()) : "new", stack);
        }
        if (initialState == null) {
            currentContext.set(new RequestContextState());
            // Fire an event with qualifier @Initialized(RequestScoped.class) if there are any observers for it
            fireIfNotEmpty(initializedNotifier);
        } else {
            if (initialState instanceof RequestContextState) {
                currentContext.set((RequestContextState) initialState);
            } else {
                throw new IllegalArgumentException("Invalid initial state: " + initialState.getClass().getName());
            }
        }
    }

    @Override
    public ContextState getState() {
        RequestContextState state = currentContext.get();
        if (state == null) {
            // Thread local not set - context is not active!
            throw notActive();
        }
        return state;
    }

    public ContextState getStateIfActive() {
        return currentContext.get();
    }

    @Override
    public void deactivate() {
        if (LOG.isTraceEnabled()) {
            String stack = Arrays.stream(Thread.currentThread().getStackTrace())
                    .skip(2)
                    .limit(7)
                    .map(se -> "\n\t" + se.toString())
                    .collect(Collectors.joining());
            LOG.tracef("Deactivate%s\n\t...", stack);
        }
        currentContext.remove();
    }

    @Override
    public void destroy() {
        destroy(currentContext.get());
    }

    @Override
    public void destroy(ContextState state) {
        if (LOG.isTraceEnabled()) {
            String stack = Arrays.stream(Thread.currentThread().getStackTrace())
                    .skip(2)
                    .limit(7)
                    .map(se -> "\n\t" + se.toString())
                    .collect(Collectors.joining());
            LOG.tracef("Destroy %s%s\n\t...", state != null ? Integer.toHexString(state.hashCode()) : "", stack);
        }
        if (state == null) {
            // nothing to destroy
            return;
        }
        if (state instanceof RequestContextState) {
            RequestContextState reqState = ((RequestContextState) state);
            if (reqState.invalidate()) {
                // Fire an event with qualifier @BeforeDestroyed(RequestScoped.class) if there are any observers for it
                fireIfNotEmpty(beforeDestroyedNotifier);
                ((RequestContextState) state).clearAfter(this::destroyContextElement);
                // Fire an event with qualifier @Destroyed(RequestScoped.class) if there are any observers for it
                fireIfNotEmpty(destroyedNotifier);
            }
        } else {
            throw new IllegalArgumentException("Invalid state implementation: " + state.getClass().getName());
        }
    }

    private void destroyContextElement(Contextual<?> contextual, ContextInstanceHandle<?> contextInstanceHandle) {
        try {
            contextInstanceHandle.destroy();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to destroy instance" + contextInstanceHandle.get(), e);
        }
    }

    private void fireIfNotEmpty(Notifier<Object> notifier) {
        if (notifier != null && !notifier.isEmpty()) {
            try {
                notifier.notify(toString());
            } catch (Exception e) {
                LOG.warn("An error occurred during delivery of the container lifecycle event for qualifiers "
                        + notifier.eventMetadata.getQualifiers(), e);
            }
        }
    }

    private ContextNotActiveException notActive() {
        String msg = "Request context is not active - you can activate the request context for a specific method using the @ActivateRequestContext interceptor binding";
        return new ContextNotActiveException(msg);
    }

    static class RequestContextState implements ContextState {

        private static final VarHandle IS_VALID;
        private static final VarHandle MAP;
        private static final VarHandle SINGLE_V_MAP;

        static {
            try {
                IS_VALID = MethodHandles.lookup().findVarHandle(RequestContextState.class, "isValid", int.class);
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
            try {
                MAP = MethodHandles.lookup().findVarHandle(RequestContextState.class, "map", ConcurrentHashMap.class);
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
            try {
                SINGLE_V_MAP = MethodHandles.lookup().findVarHandle(RequestContextState.class, "singleValueMap",
                        CtxHandle.class);
            } catch (ReflectiveOperationException e) {
                throw new Error(e);
            }
        }

        private static final class CtxHandle {
            private static final CtxHandle EMPTY_CTX_HANDLE = new CtxHandle(null, null);
            private final Contextual<?> ctx;
            private final ContextInstanceHandle<?> handle;

            private CtxHandle(final Contextual<?> ctx, final ContextInstanceHandle<?> handle) {
                this.ctx = ctx;
                this.handle = handle;
            }

            public Contextual<?> getCtx() {
                if (isEmpty()) {
                    throw new IllegalStateException("this value is not available for the empty instance");
                }
                return ctx;
            }

            public ContextInstanceHandle<?> getHandle() {
                if (isEmpty()) {
                    throw new IllegalStateException("this value is not available for the empty instance");
                }
                return handle;
            }

            public boolean containsKey(Contextual<?> key) {
                if (isEmpty()) {
                    return false;
                }
                final Contextual<?> ctx = this.ctx;
                return key == ctx || key.equals(ctx);
            }

            public ContextInstanceHandle<?> get(Contextual<?> key) {
                if (isEmpty()) {
                    return null;
                }
                final Contextual<?> ctx = this.ctx;
                return (key == ctx || key.equals(ctx)) ? handle : null;
            }

            public void forEach(BiConsumer<Contextual<?>, ContextInstanceHandle<?>> consumer) {
                if (isEmpty()) {
                    return;
                }
                consumer.accept(ctx, handle);
            }

            public boolean isEmpty() {
                return this == CtxHandle.EMPTY_CTX_HANDLE;
            }

            public static CtxHandle empty() {
                return CtxHandle.EMPTY_CTX_HANDLE;
            }

            public static CtxHandle of(final Contextual<?> ctx, final ContextInstanceHandle<?> handle) {
                Objects.requireNonNull(ctx);
                Objects.requireNonNull(handle);
                return new CtxHandle(ctx, handle);
            }

            public Map<InjectableBean<?>, Object> getContextualInstanceAsMap() {
                if (isEmpty()) {
                    return Collections.emptyMap();
                }
                final ContextInstanceHandle<?> handle = this.handle;
                return Collections.singletonMap(handle.getBean(), handle.get());
            }
        }

        private volatile ConcurrentHashMap<Contextual<?>, ContextInstanceHandle<?>> map;
        private volatile CtxHandle singleValueMap;
        private volatile int isValid;

        RequestContextState() {
            // no need to use any volatile set-like here for safe publication: too costy!!
            // this map's lazy set shouldn't be needed with null values, but better be safe then sorry!
            MAP.set(this, null);
            SINGLE_V_MAP.set(this, CtxHandle.empty());
            IS_VALID.set(this, 1);
            VarHandle.storeStoreFence();
        }

        public ContextInstanceHandle<?> get(Contextual<?> key) {
            final var singleValueMap = this.singleValueMap;
            if (singleValueMap != null) {
                return singleValueMap.get(key);
            }
            return spinWaitMap().get(key);
        }

        /**
         * Current algorithm allows a "bubble" where singleValueMap is NULL, but map isn't yet
         * migrated: it assumes the value will be moved soon.
         * IS NOT RECOMMENDED to make this algorithm more cooperative to save weird ordering issue
         * due to a cooperative map allocation that doesn't have the information of the existing
         * singleValueMap's value.
         */
        private Map<Contextual<?>, ContextInstanceHandle<?>> spinWaitMap() {
            Map<Contextual<?>, ContextInstanceHandle<?>> map;
            while ((map = this.map) == null) {
                Thread.onSpinWait();
            }
            return map;
        }

        public void clearAfter(BiConsumer<Contextual<?>, ContextInstanceHandle<?>> consumer) {
            for (;;) {
                final var singleValueMap = this.singleValueMap;
                if (singleValueMap != null) {
                    if (singleValueMap.isEmpty()) {
                        return;
                    }
                    // this is necessary to save a migration to map to "resurrect"
                    // the existing entries
                    if (SINGLE_V_MAP.compareAndSet(this, singleValueMap, CtxHandle.empty())) {
                        singleValueMap.forEach(consumer);
                        return;
                    }
                } else {
                    final var map = spinWaitMap();
                    map.forEach(consumer);
                    map.clear();
                    return;
                }
            }
        }

        public void put(Contextual<?> key, ContextInstanceHandle<?> value) {
            for (;;) {
                final var singleValueMap = this.singleValueMap;
                if (singleValueMap != null) {
                    if (!singleValueMap.isEmpty() && !singleValueMap.containsKey(key)) {
                        if (!tryMigrateSingleMap(singleValueMap, key, value)) {
                            continue;
                        }
                        return;
                    }
                    // single v map is empty or contains the value: in both cases requires being replaced
                    if (SINGLE_V_MAP.compareAndSet(this, singleValueMap, CtxHandle.of(key, value))) {
                        return;
                    }
                } else {
                    spinWaitMap().put(key, value);
                }
            }
        }

        private boolean tryMigrateSingleMap(CtxHandle singleValueMap, Contextual<?> key,
                ContextInstanceHandle<?> value) {
            assert !singleValueMap.isEmpty() && !singleValueMap.containsKey(key);
            if (!SINGLE_V_MAP.compareAndSet(this, singleValueMap, null)) {
                return false;
            }
            // we're in charge to migrate the single value and allocate the map
            final var map = new ConcurrentHashMap<Contextual<?>, ContextInstanceHandle<?>>(2, 0.75f, 1);
            map.put(singleValueMap.getCtx(), singleValueMap.getHandle());
            map.put(key, value);
            MAP.setRelease(this, map);
            return true;
        }

        public ContextInstanceHandle<?> remove(Contextual<?> key) {
            // MUST HOLD: removal cannot switch back from map to single value map
            for (;;) {
                final var singleValueMap = this.singleValueMap;
                if (singleValueMap != null) {
                    final var value = singleValueMap.get(key);
                    if (value == null) {
                        return null;
                    }
                    if (!SINGLE_V_MAP.compareAndSet(this, singleValueMap, CtxHandle.empty())) {
                        continue;
                    }
                    return value;
                } else {
                    return spinWaitMap().remove(key);
                }
            }
        }

        @Override
        public Map<InjectableBean<?>, Object> getContextualInstances() {
            final var singleValueMap = this.singleValueMap;
            if (singleValueMap != null) {
                return singleValueMap.getContextualInstanceAsMap();
            }
            return spinWaitMap().values().stream()
                    .collect(Collectors.toUnmodifiableMap(ContextInstanceHandle::getBean, ContextInstanceHandle::get));
        }

        /**
         * @return {@code true} if the state was successfully invalidated, {@code false} otherwise
         */
        boolean invalidate() {
            // Atomically sets the value just like AtomicBoolean.compareAndSet(boolean, boolean)
            return IS_VALID.compareAndSet(this, 1, 0);
        }

        @Override
        public boolean isValid() {
            return isValid == 1;
        }

    }

}
