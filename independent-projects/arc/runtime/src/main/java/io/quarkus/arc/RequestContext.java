package io.quarkus.arc;

import io.quarkus.arc.EventImpl.Notifier;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
    private final ThreadLocal<ConcurrentMap<Contextual<?>, ContextInstanceHandle<?>>> currentContext = new ThreadLocal<>();

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
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        Map<Contextual<?>, ContextInstanceHandle<?>> ctx = currentContext.get();
        if (ctx == null) {
            // Thread local not set - context is not active!
            throw new ContextNotActiveException();
        }
        ContextInstanceHandle<T> instance = (ContextInstanceHandle<T>) ctx.get(contextual);
        if (instance == null && creationalContext != null) {
            // Bean instance does not exist - create one if we have CreationalContext
            instance = new ContextInstanceHandleImpl<T>((InjectableBean<T>) contextual,
                    contextual.create(creationalContext), creationalContext);
            ctx.put(contextual, instance);
        }
        return instance != null ? instance.get() : null;
    }

    @Override
    public <T> T get(Contextual<T> contextual) {
        return get(contextual, null);
    }

    @Override
    public boolean isActive() {
        return currentContext.get() != null;
    }

    @Override
    public void destroy(Contextual<?> contextual) {
        Map<Contextual<?>, ContextInstanceHandle<?>> ctx = currentContext.get();
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
            currentContext.set(new ConcurrentHashMap<>());
        } else {
            if (initialState instanceof RequestContextState) {
                currentContext.set(((RequestContextState) initialState).value);
            } else {
                throw new IllegalArgumentException("Invalid inital state: " + initialState);
            }
        }
        // Fire an event with qualifier @Initialized(RequestScoped.class) if there are any observers for it
        fireIfNotEmpty(initializedNotifier);
    }

    @Override
    public InjectableContext.ContextState getState() {
        ConcurrentMap<Contextual<?>, ContextInstanceHandle<?>> ctx = currentContext.get();
        if (ctx == null) {
            // Thread local not set - context is not active!
            throw new ContextNotActiveException();
        }
        return new RequestContextState(ctx);
    }

    @Override
    public void deactivate() {
        currentContext.remove();
    }

    @Override
    public void destroy() {
        Map<Contextual<?>, ContextInstanceHandle<?>> ctx = currentContext.get();
        if (ctx != null) {
            synchronized (ctx) {
                // Fire an event with qualifier @BeforeDestroyed(RequestScoped.class) if there are any observers for it
                try {
                    fireIfNotEmpty(beforeDestroyedNotifier);
                } catch (Exception e) {
                    LOGGER.warn("An error occured during delivery of the @BeforeDestroyed(RequestScoped.class) event", e);
                }
                for (InstanceHandle<?> instance : ctx.values()) {
                    try {
                        instance.destroy();
                    } catch (Exception e) {
                        throw new IllegalStateException("Unable to destroy instance" + instance.get(), e);
                    }
                }
                // Fire an event with qualifier @Destroyed(RequestScoped.class) if there are any observers for it
                try {
                    fireIfNotEmpty(destroyedNotifier);
                } catch (Exception e) {
                    LOGGER.warn("An error occured during delivery of the @Destroyed(RequestScoped.class) event", e);
                }
                ctx.clear();
            }
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
                ArcContainerImpl.instance());
    }

    private static Notifier<Object> createBeforeDestroyedNotifier() {
        return EventImpl.createNotifier(Object.class, Object.class,
                new HashSet<>(Arrays.asList(BeforeDestroyed.Literal.REQUEST, Any.Literal.INSTANCE)),
                ArcContainerImpl.instance());
    }

    private static Notifier<Object> createDestroyedNotifier() {
        return EventImpl.createNotifier(Object.class, Object.class,
                new HashSet<>(Arrays.asList(Destroyed.Literal.REQUEST, Any.Literal.INSTANCE)),
                ArcContainerImpl.instance());
    }

    private static class RequestContextState implements ContextState {

        private final ConcurrentMap<Contextual<?>, ContextInstanceHandle<?>> value;

        RequestContextState(ConcurrentMap<Contextual<?>, ContextInstanceHandle<?>> value) {
            this.value = value;
        }

        @Override
        public Map<InjectableBean<?>, Object> getContextualInstances() {
            return value.values().stream()
                    .collect(Collectors.toMap(ContextInstanceHandle::getBean, ContextInstanceHandle::get));
        }

    }
}
