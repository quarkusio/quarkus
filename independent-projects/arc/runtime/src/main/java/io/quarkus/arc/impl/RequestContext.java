package io.quarkus.arc.impl;

import io.quarkus.arc.ContextInstanceHandle;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.impl.EventImpl.Notifier;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import org.jboss.logging.Logger;

/**
 * The built-in context for {@link RequestScoped}.
 *
 * @author Martin Kouba
 */
class RequestContext implements ManagedContext {

    private static final Logger LOGGER = Logger.getLogger(RequestContext.class.getPackage().getName());

    // It's a normal scope so there may be no more than one mapped instance per contextual type per thread
    private final ThreadLocal<RequestContextState> currentContext = new ThreadLocal<>();

    private final LazyValue<Notifier<Object>> initializedNotifier;
    private final LazyValue<Notifier<Object>> beforeDestroyedNotifier;
    private final LazyValue<Notifier<Object>> destroyedNotifier;

    public RequestContext() {
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
            // Thread local not set - context is not active!
            return null;
        }
        Map<Contextual<?>, ContextInstanceHandle<?>> ctxMap = currentContext.get().value;
        ContextInstanceHandle<T> instance = (ContextInstanceHandle<T>) ctxMap.get(contextual);
        if (instance == null) {
            CreationalContext<T> creationalContext = creationalContextFun.apply(contextual);
            // Bean instance does not exist - create one if we have CreationalContext
            instance = new ContextInstanceHandleImpl<T>((InjectableBean<T>) contextual,
                    contextual.create(creationalContext), creationalContext);
            ctxMap.put(contextual, instance);
        }
        return instance.get();
    }

    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        T result = getIfActive(contextual,
                CreationalContextImpl.unwrap(Objects.requireNonNull(creationalContext, "CreationalContext must not be null")));
        if (result == null) {
            // Thread local not set - context is not active!
            throw new ContextNotActiveException();
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
        Map<Contextual<?>, ContextInstanceHandle<?>> ctx = currentContext.get().value;
        if (ctx == null) {
            // Thread local not set - context is not active!
            throw new ContextNotActiveException();
        }
        ContextInstanceHandle<T> instance = (ContextInstanceHandle<T>) ctx.get(contextual);
        return instance == null ? null : instance.get();
    }

    @Override
    public boolean isActive() {
        return currentContext.get() != null;
    }

    @Override
    public void destroy(Contextual<?> contextual) {
        Map<Contextual<?>, ContextInstanceHandle<?>> ctx = currentContext.get().value;
        if (ctx == null) {
            // Thread local not set - context is not active!
            throw new ContextNotActiveException();
        }
        ContextInstanceHandle<?> instance = ctx.remove(contextual);
        if (instance != null) {
            instance.destroy();
        }
    }

    @Override
    public void activate(ContextState initialState) {
        if (initialState == null) {
            currentContext.set(new RequestContextState(new ConcurrentHashMap<>()));
            // Fire an event with qualifier @Initialized(RequestScoped.class) if there are any observers for it
            fireIfNotEmpty(initializedNotifier);
        } else {
            if (initialState instanceof RequestContextState) {
                currentContext.set(((RequestContextState) initialState));
            } else {
                throw new IllegalArgumentException("Invalid initial state: " + initialState.getClass().getName());
            }
        }
    }

    @Override
    public ContextState getState() {
        RequestContextState ctx = currentContext.get();
        if (ctx == null) {
            // Thread local not set - context is not active!
            throw new ContextNotActiveException();
        }
        return ctx;
    }

    public ContextState getStateIfActive() {
        RequestContextState ctx = currentContext.get();
        if (ctx == null) {
            return null;
        }
        return ctx;
    }

    @Override
    public void deactivate() {
        currentContext.remove();
    }

    @Override
    public void destroy() {
        destroy(currentContext.get());
    }

    @Override
    public void destroy(ContextState state) {
        if (state == null) {
            // nothing to destroy
            return;
        }
        if (state instanceof RequestContextState) {
            RequestContextState reqState = ((RequestContextState) state);
            reqState.isValid.set(false);
            destroy(reqState.value);
        } else {
            throw new IllegalArgumentException("Invalid state: " + state.getClass().getName());
        }
    }

    private void destroy(Map<Contextual<?>, ContextInstanceHandle<?>> currentContext) {
        if (currentContext != null) {
            synchronized (currentContext) {
                // Fire an event with qualifier @BeforeDestroyed(RequestScoped.class) if there are any observers for it
                try {
                    fireIfNotEmpty(beforeDestroyedNotifier);
                } catch (Exception e) {
                    LOGGER.warn("An error occurred during delivery of the @BeforeDestroyed(RequestScoped.class) event", e);
                }
                //Performance: avoid an iterator on the map elements
                currentContext.forEach(this::destroyContextElement);
                // Fire an event with qualifier @Destroyed(RequestScoped.class) if there are any observers for it
                try {
                    fireIfNotEmpty(destroyedNotifier);
                } catch (Exception e) {
                    LOGGER.warn("An error occurred during delivery of the @Destroyed(RequestScoped.class) event", e);
                }
                currentContext.clear();
            }
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

        private final ConcurrentMap<Contextual<?>, ContextInstanceHandle<?>> value;
        private final AtomicBoolean isValid;

        RequestContextState(ConcurrentMap<Contextual<?>, ContextInstanceHandle<?>> value) {
            this.value = value;
            this.isValid = new AtomicBoolean(true);
        }

        @Override
        public Map<InjectableBean<?>, Object> getContextualInstances() {
            return value.values().stream()
                    .collect(Collectors.toUnmodifiableMap(ContextInstanceHandle::getBean, ContextInstanceHandle::get));
        }

        @Override
        public boolean isValid() {
            return isValid.get();
        }

    }

}
