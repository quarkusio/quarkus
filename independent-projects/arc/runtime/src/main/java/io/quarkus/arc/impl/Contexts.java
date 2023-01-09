package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Singleton;

import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.ManagedContext;

/**
 * Encapsulates all contexts used in the container.
 * <p>
 * The idea is to avoid unnecessary map usage for built-in contexts.
 */
class Contexts {

    // Built-in contexts
    final ManagedContext requestContext;
    final InjectableContext applicationContext;
    final InjectableContext singletonContext;

    // Used to optimize the getContexts(Class<? extends Annotation>)
    private final List<InjectableContext> applicationContextSingleton;
    private final List<InjectableContext> singletonContextSingleton;
    private final List<InjectableContext> requestContextSingleton;

    // Lazily computed list of contexts for a scope
    private final ClassValue<List<InjectableContext>> unoptimizedContexts;

    // Precomputed values
    final Set<Class<? extends Annotation>> scopes;

    Contexts(ManagedContext requestContext, InjectableContext applicationContext,
            InjectableContext singletonContext, Map<Class<? extends Annotation>, List<InjectableContext>> contexts) {
        this.requestContext = requestContext;
        this.applicationContext = applicationContext;
        this.singletonContext = singletonContext;

        this.applicationContextSingleton = Collections.singletonList(applicationContext);
        this.singletonContextSingleton = Collections.singletonList(singletonContext);
        List<InjectableContext> requestContexts = contexts.get(RequestScoped.class);
        this.requestContextSingleton = requestContexts != null ? requestContexts : Collections.singletonList(requestContext);

        if (!contexts.isEmpty()) {
            // At least one custom context is registered
            int mapSize = contexts.size();
            @SuppressWarnings("unchecked")
            Class<? extends Annotation>[] keys = new Class[mapSize];
            @SuppressWarnings("unchecked")
            List<InjectableContext>[] values = new List[mapSize];
            int index = 0;
            for (Map.Entry<Class<? extends Annotation>, List<InjectableContext>> entry : contexts.entrySet()) {
                keys[index] = entry.getKey();
                values[index] = entry.getValue();
                index++;
            }
            this.unoptimizedContexts = new ClassValue<>() {
                @Override
                protected List<InjectableContext> computeValue(Class<?> type) {
                    for (int i = 0; i < mapSize; i++) {
                        if (keys[i].equals(type)) {
                            return values[i];
                        }
                    }
                    return Collections.emptyList();
                }
            };
            Set<Class<? extends Annotation>> all = new HashSet<>(contexts.keySet());
            all.add(ApplicationScoped.class);
            all.add(Singleton.class);
            all.add(RequestScoped.class);
            this.scopes = Set.copyOf(all);
        } else {
            // No custom context is registered
            this.unoptimizedContexts = null;
            this.scopes = Set.of(ApplicationScoped.class, Singleton.class, RequestScoped.class);
        }
    }

    InjectableContext getActiveContext(Class<? extends Annotation> scopeType) {
        // Application/Singleton context is always active and it's not possible to register a custom context for these scopes
        if (ApplicationScoped.class.equals(scopeType)) {
            return applicationContext;
        } else if (Singleton.class.equals(scopeType)) {
            return singletonContext;
        }
        List<InjectableContext> contextsForScope = getContexts(scopeType);
        InjectableContext selected = null;
        for (InjectableContext context : contextsForScope) {
            if (context.isActive()) {
                if (selected != null) {
                    throw new IllegalArgumentException(
                            "More than one context object for the given scope: " + selected + " " + context);
                }
                selected = context;
            }
        }
        return selected;
    }

    List<InjectableContext> getContexts(Class<? extends Annotation> scopeType) {
        // Optimize for buil-in normal scopes - this method is used internally during client proxy invocation
        if (ApplicationScoped.class.equals(scopeType)) {
            return applicationContextSingleton;
        } else if (RequestScoped.class.equals(scopeType)) {
            return requestContextSingleton;
        } else if (Singleton.class.equals(scopeType)) {
            return singletonContextSingleton;
        }
        return unoptimizedContexts != null ? unoptimizedContexts.get(scopeType) : Collections.emptyList();
    }

    static class Builder {

        private final ManagedContext requestContext;
        private final InjectableContext applicationContext;
        private final InjectableContext singletonContext;
        private final Map<Class<? extends Annotation>, List<InjectableContext>> contexts = new HashMap<>();

        public Builder(ManagedContext requestContext, InjectableContext applicationContext,
                InjectableContext singletonContext) {
            this.requestContext = requestContext;
            this.applicationContext = applicationContext;
            this.singletonContext = singletonContext;
        }

        Builder putContext(InjectableContext context) {
            List<InjectableContext> values = contexts.get(context.getScope());
            if (values == null) {
                contexts.put(context.getScope(), Collections.singletonList(context));
            } else {
                List<InjectableContext> multi = new ArrayList<>(values.size() + 1);
                multi.addAll(values);
                multi.add(context);
                contexts.put(context.getScope(), List.copyOf(multi));
            }
            return this;
        }

        Contexts build() {
            if (contexts.containsKey(RequestScoped.class)) {
                // If a custom request context is registered then add the built-in context as well
                putContext(requestContext);
            }
            return new Contexts(requestContext, applicationContext, singletonContext, contexts);
        }

    }

}
