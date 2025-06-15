package io.quarkus.undertow.runtime;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Event;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

import org.jboss.logging.Logger;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ContextInstanceHandle;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.impl.ComputingCache;
import io.quarkus.arc.impl.ContextInstanceHandleImpl;
import io.undertow.servlet.handlers.ServletRequestContext;

@WebListener
public class HttpSessionContext implements InjectableContext, HttpSessionListener {

    private static final String CONTEXTUAL_INSTANCES_KEY = HttpSessionContext.class.getName() + ".contextualInstances";

    private static final ThreadLocal<HttpSession> DESTRUCT_SESSION = new ThreadLocal<>();

    private static final Logger LOG = Logger.getLogger(HttpSessionContext.class);

    @Override
    public Class<? extends Annotation> getScope() {
        return SessionScoped.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        HttpSession session = session(true);
        if (session == null) {
            throw new ContextNotActiveException();
        }
        InjectableBean<T> bean = (InjectableBean<T>) contextual;
        ComputingCache<Key, ContextInstanceHandle<?>> contextualInstances = getContextualInstances(session);
        if (creationalContext != null) {
            return (T) contextualInstances.getValue(new Key(creationalContext, bean.getIdentifier())).get();
        } else {
            InstanceHandle<T> handle = (InstanceHandle<T>) contextualInstances
                    .getValueIfPresent(Key.of(bean.getIdentifier()));
            return handle != null ? handle.get() : null;
        }
    }

    @Override
    public <T> T get(Contextual<T> contextual) {
        return get(contextual, null);
    }

    @Override
    public boolean isActive() {
        return session(true) != null;
    }

    @Override
    public ContextState getState() {
        return new ContextState() {

            @Override
            public Map<InjectableBean<?>, Object> getContextualInstances() {
                HttpSession session = session(false);
                if (session != null) {
                    return HttpSessionContext.this.getContextualInstances(session).getPresentValues().stream()
                            .collect(Collectors.toMap(ContextInstanceHandle::getBean, ContextInstanceHandle::get));
                }
                return Collections.emptyMap();
            }
        };
    }

    @Override
    public void destroy(Contextual<?> contextual) {
        HttpSession session = session(true);
        if (session == null) {
            throw new ContextNotActiveException();
        }
        InjectableBean<?> bean = (InjectableBean<?>) contextual;
        InstanceHandle<?> instanceHandle = getContextualInstances(session).remove(Key.of(bean.getIdentifier()));
        if (instanceHandle != null) {
            instanceHandle.destroy();
        }
    }

    @Override
    public void destroy() {
        HttpSession session = session(true);
        if (session == null) {
            throw new ContextNotActiveException();
        }
        destroy(session);
    }

    private void destroy(HttpSession session) {
        synchronized (this) {
            ComputingCache<Key, ContextInstanceHandle<?>> instances = getContextualInstances(session);
            for (ContextInstanceHandle<?> instance : instances.getPresentValues()) {
                // try to remove the contextual instance from the context
                ContextInstanceHandle<?> val = instances.remove(Key.of(instance.getBean().getIdentifier()));
                if (val != null) {
                    // destroy it afterwards
                    try {
                        val.destroy();
                    } catch (Exception e) {
                        LOG.errorf(e, "Unable to destroy bean instance: %s", val.get());
                    }
                }
            }
            if (!instances.isEmpty()) {
                LOG.warnf(
                        "Some @SessionScoped beans were created during destruction of the session context: %s\n\t- potential @PreDestroy callbacks declared on the beans were not invoked\n\t- in general, @SessionScoped beans should not call other @SessionScoped beans in a @PreDestroy callback",
                        instances.getPresentValues().stream().map(ContextInstanceHandle::getBean)
                                .collect(Collectors.toList()));
            }
            instances.clear();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private ComputingCache<Key, ContextInstanceHandle<?>> getContextualInstances(HttpSession session) {
        ComputingCache<Key, ContextInstanceHandle<?>> contextualInstances = (ComputingCache<Key, ContextInstanceHandle<?>>) session
                .getAttribute(CONTEXTUAL_INSTANCES_KEY);
        if (contextualInstances == null) {
            synchronized (this) {
                contextualInstances = (ComputingCache<Key, ContextInstanceHandle<?>>) session
                        .getAttribute(CONTEXTUAL_INSTANCES_KEY);
                if (contextualInstances == null) {
                    contextualInstances = new ComputingCache<>(key -> {
                        InjectableBean bean = Arc.container().bean(key.beanIdentifier);
                        if (key.creationalContext == null) {
                            throw new IllegalStateException("Cannot create bean ");
                        }
                        return new ContextInstanceHandleImpl(bean, bean.create(key.creationalContext),
                                key.creationalContext);
                    });
                    session.setAttribute(CONTEXTUAL_INSTANCES_KEY, contextualInstances);
                }
            }
        }
        return contextualInstances;
    }

    private HttpSession session(boolean create) {
        HttpSession session = null;
        try {
            session = ((HttpServletRequest) ServletRequestContext.requireCurrent().getServletRequest())
                    .getSession(create);
        } catch (IllegalStateException ignored) {
            session = DESTRUCT_SESSION.get();
        }
        return session;
    }

    static class Key {

        static Key of(String beanIdentifier) {
            return new Key(null, beanIdentifier);
        }

        CreationalContext<?> creationalContext;

        String beanIdentifier;

        public Key(CreationalContext<?> creationalContext, String beanIdentifier) {
            this.creationalContext = creationalContext;
            this.beanIdentifier = beanIdentifier;
        }

        @Override
        public int hashCode() {
            return Objects.hash(beanIdentifier);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Key other = (Key) obj;
            return Objects.equals(beanIdentifier, other.beanIdentifier);
        }

    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        Event<Object> event = Arc.container().beanManager().getEvent();
        event.select(HttpSession.class, BeforeDestroyed.Literal.SESSION).fire(session);
        try {
            DESTRUCT_SESSION.set(session);
            destroy(session);
            event.select(HttpSession.class, Destroyed.Literal.SESSION).fire(session);
        } finally {
            DESTRUCT_SESSION.remove();
        }
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        Arc.container().beanManager().getEvent().select(HttpSession.class, Initialized.Literal.SESSION)
                .fire(se.getSession());
    }

}
