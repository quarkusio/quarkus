package io.quarkus.arc.impl;

import io.quarkus.arc.ContextInstanceHandle;
import io.quarkus.arc.CurrentContext;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.impl.EventImpl.Notifier;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.Any;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

/**
 * The built-in context for {@link RequestScoped}.
 *
 * @author Martin Kouba
 */
class RequestContext implements ManagedContext {

    private static final Logger LOG = Logger.getLogger("io.quarkus.arc.requestContext");

    private final CurrentContext<RequestContextState> currentContext;

    private final LazyValue<Notifier<Object>> initializedNotifier;
    private final LazyValue<Notifier<Object>> beforeDestroyedNotifier;
    private final LazyValue<Notifier<Object>> destroyedNotifier;

    public RequestContext(CurrentContext<RequestContextState> currentContext) {
        this.currentContext = currentContext;
        this.initializedNotifier = new LazyValue<>(RequestContext::createInitializedNotifier);
        this.beforeDestroyedNotifier = new LazyValue<>(RequestContext::createBeforeDestroyedNotifier);
        this.destroyedNotifier = new LazyValue<>(RequestContext::createDestroyedNotifier);
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
        ContextInstanceHandle<T> instance = (ContextInstanceHandle<T>) ctxState.map.get(contextual);
        if (instance == null) {
            CreationalContext<T> creationalContext = creationalContextFun.apply(contextual);
            // Bean instance does not exist - create one if we have CreationalContext
            instance = new ContextInstanceHandleImpl<T>((InjectableBean<T>) contextual,
                    contextual.create(creationalContext), creationalContext);
            ctxState.map.put(contextual, instance);
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
        ContextInstanceHandle<T> instance = (ContextInstanceHandle<T>) state.map.get(contextual);
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
        ContextInstanceHandle<?> instance = state.map.remove(contextual);
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
            currentContext.set(new RequestContextState(new ConcurrentHashMap<>()));
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
            reqState.isValid = false;
            synchronized (state) {
                Map<Contextual<?>, ContextInstanceHandle<?>> map = ((RequestContextState) state).map;
                // Fire an event with qualifier @BeforeDestroyed(RequestScoped.class) if there are any observers for it
                try {
                    fireIfNotEmpty(beforeDestroyedNotifier);
                } catch (Exception e) {
                    LOG.warn("An error occurred during delivery of the @BeforeDestroyed(RequestScoped.class) event", e);
                }
                //Performance: avoid an iterator on the map elements
                map.forEach(this::destroyContextElement);
                // Fire an event with qualifier @Destroyed(RequestScoped.class) if there are any observers for it
                try {
                    fireIfNotEmpty(destroyedNotifier);
                } catch (Exception e) {
                    LOG.warn("An error occurred during delivery of the @Destroyed(RequestScoped.class) event", e);
                }
                map.clear();
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

    private void fireIfNotEmpty(LazyValue<Notifier<Object>> value) {
        Notifier<Object> notifier = value.get();
        if (!notifier.isEmpty()) {
            notifier.notify(toString());
        }
    }

    private ContextNotActiveException notActive() {
        String msg = "Request context is not active - you can activate the request context for a specific method using the @ActivateRequestContext interceptor binding";
        return new ContextNotActiveException(msg);
    }

    private static Notifier<Object> createInitializedNotifier() {
        return EventImpl.createNotifier(Object.class, Object.class,
                new HashSet<>(Arrays.asList(Initialized.Literal.REQUEST, Any.Literal.INSTANCE)),
                ArcContainerImpl.instance(), false);
    }

    private static Notifier<Object> createBeforeDestroyedNotifier() {
        return EventImpl.createNotifier(Object.class, Object.class,
                new HashSet<>(Arrays.asList(BeforeDestroyed.Literal.REQUEST, Any.Literal.INSTANCE)),
                ArcContainerImpl.instance(), false);
    }

    private static Notifier<Object> createDestroyedNotifier() {
        return EventImpl.createNotifier(Object.class, Object.class,
                new HashSet<>(Arrays.asList(Destroyed.Literal.REQUEST, Any.Literal.INSTANCE)),
                ArcContainerImpl.instance(), false);
    }

    static class RequestContextState implements ContextState {

        private final Map<Contextual<?>, ContextInstanceHandle<?>> map;

        private volatile boolean isValid;

        RequestContextState(ConcurrentMap<Contextual<?>, ContextInstanceHandle<?>> value) {
            this.map = Objects.requireNonNull(value);
            this.isValid = true;
        }

        @Override
        public Map<InjectableBean<?>, Object> getContextualInstances() {
            return map.values().stream()
                    .collect(Collectors.toUnmodifiableMap(ContextInstanceHandle::getBean, ContextInstanceHandle::get));
        }

        @Override
        public boolean isValid() {
            return isValid;
        }

    }

}
