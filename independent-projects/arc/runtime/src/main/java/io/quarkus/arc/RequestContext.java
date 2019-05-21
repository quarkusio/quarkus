/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.arc;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;

/**
 * The built-in context for {@link RequestScoped}.
 *
 * @author Martin Kouba
 */
class RequestContext implements ManagedContext {

    // It's a normal scope so there may be no more than one mapped instance per contextual type per thread
    private final ThreadLocal<Map<Contextual<?>, ContextInstanceHandle<?>>> currentContext = new ThreadLocal<>();

    private final LazyValue<EventImpl.Notifier<Object>> initializedNotifier;
    private final LazyValue<EventImpl.Notifier<Object>> beforeDestroyedNotifier;
    private final LazyValue<EventImpl.Notifier<Object>> destroyedNotifier;

    public RequestContext() {
        this.initializedNotifier = new LazyValue<>(() -> EventImpl.createNotifier(Object.class, Object.class,
                new HashSet<>(Arrays.asList(Initialized.Literal.REQUEST, Any.Literal.INSTANCE)),
                ArcContainerImpl.instance()));
        this.beforeDestroyedNotifier = new LazyValue<>(() -> EventImpl.createNotifier(Object.class, Object.class,
                new HashSet<>(Arrays.asList(BeforeDestroyed.Literal.REQUEST, Any.Literal.INSTANCE)),
                ArcContainerImpl.instance()));
        this.destroyedNotifier = new LazyValue<>(() -> EventImpl.createNotifier(Object.class, Object.class,
                new HashSet<>(Arrays.asList(Destroyed.Literal.REQUEST, Any.Literal.INSTANCE)),
                ArcContainerImpl.instance()));
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
    public Collection<ContextInstanceHandle<?>> getAll() {
        Map<Contextual<?>, ContextInstanceHandle<?>> ctx = currentContext.get();
        if (ctx == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(ctx.values());
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

    private void fireIfNotEmpty(LazyValue<EventImpl.Notifier<Object>> value) {
        EventImpl.Notifier notifier = value.get();
        if (!notifier.isEmpty()) {
            notifier.notify(toString());
        }
    }

    @Override
    public void deactivate() {
        currentContext.remove();
    }

    @Override
    public void activate(Collection<ContextInstanceHandle<?>> initialState) {
        Map<Contextual<?>, ContextInstanceHandle<?>> state = new HashMap<>();
        if (initialState != null) {
            for (ContextInstanceHandle<?> instanceHandle : initialState) {
                if (!instanceHandle.getBean().getScope().equals(getScope())) {
                    throw new IllegalArgumentException("Invalid bean scope: " + instanceHandle.getBean());
                }
                state.put(instanceHandle.getBean(), instanceHandle);
            }
        }
        currentContext.set(state);
        // Fire an event with qualifier @Initialized(RequestScoped.class) if there are any observers for it
        fireIfNotEmpty(initializedNotifier);
    }

    @Override
    public void destroy() {
        Map<Contextual<?>, ContextInstanceHandle<?>> ctx = currentContext.get();
        if (ctx != null) {
            synchronized (ctx) {
                // Fire an event with qualifier @BeforeDestroyed(RequestScoped.class) if there are any observers for it
                fireIfNotEmpty(beforeDestroyedNotifier);
                for (InstanceHandle<?> instance : ctx.values()) {
                    try {
                        instance.destroy();
                    } catch (Exception e) {
                        throw new IllegalStateException("Unable to destroy instance" + instance.get(), e);
                    }
                }
                // Fire an event with qualifier @Destroyed(RequestScoped.class) if there are any observers for it
                fireIfNotEmpty(destroyedNotifier);
                ctx.clear();
            }
        }
    }
}
