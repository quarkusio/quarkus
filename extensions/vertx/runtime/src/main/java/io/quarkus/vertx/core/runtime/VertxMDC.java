package io.quarkus.vertx.core.runtime;

import static io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle.setContextSafe;
import static io.smallrye.common.vertx.VertxContext.getOrCreateDuplicatedContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jboss.logmanager.MDCProvider;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;

public enum VertxMDC implements MDCProvider {
    INSTANCE;

    final InheritableThreadLocal<Map<String, Object>> inheritableThreadLocalMap = new InheritableThreadLocal<>() {
        @Override
        protected Map<String, Object> childValue(Map<String, Object> parentValue) {
            if (parentValue == null) {
                return null;
            }
            return new HashMap<>(parentValue);
        }

        @Override
        protected Map<String, Object> initialValue() {
            return new HashMap<>();
        }
    };

    /**
     * Get the value for a key, or {@code null} if there is no mapping.
     *
     * Tries to use the current Vert.x Context, if the context is non-existent
     * meaning that it was called out of a Vert.x thread it will fall back to
     * the thread local context map.
     *
     * @param key the key
     * @return the value
     */
    @Override
    public String get(String key) {
        return get(key, getContext());
    }

    /**
     * Get the value for a key, or {@code null} if there is no mapping.
     *
     * Tries to use the current Vert.x Context, if the context is non-existent
     * meaning that it was called out of a Vert.x thread it will fall back to
     * the thread local context map.
     *
     * @param key the key
     * @return the value
     */
    @Override
    public Object getObject(String key) {
        return getObject(key, getContext());
    }

    /**
     * Get the value for a key the in specified Context, or {@code null} if there is no mapping.
     * If the informed context is null it falls back to the thread local context map.
     *
     * @param key the key
     * @param vertxContext the context
     * @return the value
     */
    public String get(String key, Context vertxContext) {
        Object value = getObject(key, vertxContext);
        return value != null ? value.toString() : null;
    }

    /**
     * Get the value for a key the in specified Context, or {@code null} if there is no mapping.
     * If the context is null it falls back to the thread local context map.
     *
     * @param key the key
     * @param vertxContext the context
     * @return the value
     */
    public Object getObject(String key, Context vertxContext) {
        Objects.requireNonNull(key);
        return contextualDataMap(vertxContext).get(key);
    }

    /**
     * Set the value of a key, returning the old value (if any) or {@code null} if there was none.
     *
     * Tries to use the current Vert.x Context, if the context is non-existent
     * meaning that it was called out of a Vert.x thread it will fall back to
     * the thread local context map.
     *
     * @param key the key
     * @param value the new value
     * @return the old value or {@code null} if there was none
     */
    @Override
    public String put(String key, String value) {
        return put(key, value, getContext());
    }

    /**
     * Set the value of a key, returning the old value (if any) or {@code null} if there was none.
     *
     * Tries to use the current Vert.x Context, if the context is non-existent
     * meaning that it was called out of a Vert.x thread it will fall back to
     * the thread local context map.
     *
     * @param key the key
     * @param value the new value
     * @return the old value or {@code null} if there was none
     */
    @Override
    public Object putObject(String key, Object value) {
        return putObject(key, value, getContext());
    }

    /**
     * Set the value of a key, returning the old value (if any) or {@code null} if there was none.
     * If the informed context is null it falls back to the thread local context map.
     *
     * @param key the key
     * @param value the new value
     * @return the old value or {@code null} if there was none
     */
    public String put(String key, String value, Context vertxContext) {
        Object oldValue = putObject(key, value, vertxContext);
        return oldValue != null ? oldValue.toString() : null;
    }

    /**
     * Set the value of a key, returning the old value (if any) or {@code null} if there was none.
     * If the informed context is null it falls back to the thread local context map.
     *
     * @param key the key
     * @param value the new value
     * @return the old value or {@code null} if there was none
     */
    public Object putObject(String key, Object value, Context vertxContext) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);
        return contextualDataMap(vertxContext).put(key, value);
    }

    /**
     * Removes a key.
     *
     * Tries to use the current Vert.x Context, if the context is non-existent
     * meaning that it was called out of a Vert.x thread it will fall back to
     * the thread local context map.
     *
     * @param key the key
     * @return the old value or {@code null} if there was none
     */
    @Override
    public String remove(String key) {
        return remove(key, getContext());
    }

    /**
     * Removes a key.
     *
     * Tries to use the current Vert.x Context, if the context is non-existent
     * meaning that it was called out of a Vert.x thread it will fall back to
     * the thread local context map.
     *
     * @param key the key
     * @return the old value or {@code null} if there was none
     */
    @Override
    public Object removeObject(String key) {
        return removeObject(key, getContext());
    }

    /**
     * Removes a key.
     * If the informed context is null it falls back to the thread local context map.
     *
     * @param key the key
     * @return the old value or {@code null} if there was none
     */
    public String remove(String key, Context vertxContext) {
        Object oldValue = removeObject(key, vertxContext);
        return oldValue != null ? oldValue.toString() : null;
    }

    /**
     * Removes a key.
     * If the informed context is null it falls back to the thread local context map.
     *
     * @param key the key
     * @return the old value or {@code null} if there was none
     */
    public Object removeObject(String key, Context vertxContext) {
        Objects.requireNonNull(key);
        return contextualDataMap(vertxContext).remove(key);
    }

    /**
     * Get a copy of the MDC map. This is a relatively expensive operation.
     *
     * Tries to use the current Vert.x Context, if the context is non-existent
     * meaning that it was called out of a Vert.x thread it will fall back to
     * the thread local context map.
     *
     * @return a copy of the map
     */
    @Override
    public Map<String, String> copy() {
        return copy(getContext());
    }

    /**
     * Get a copy of the MDC map. This is a relatively expensive operation.
     *
     * Tries to use the current Vert.x Context, if the context is non-existent
     * meaning that it was called out of a Vert.x thread it will fall back to
     * the thread local context map.
     *
     * @return a copy of the map
     */
    @Override
    public Map<String, Object> copyObject() {
        return copyObject(getContext());
    }

    /**
     * Get a copy of the MDC map. This is a relatively expensive operation.
     * If the informed context is null it falls back to the thread local context map.
     *
     * @return a copy of the map
     */
    public Map<String, String> copy(Context vertxContext) {
        final HashMap<String, String> result = new HashMap<>();
        Map<String, Object> contextualDataMap = contextualDataMap(vertxContext);
        for (Map.Entry<String, Object> entry : contextualDataMap.entrySet()) {
            result.put(entry.getKey(), entry.getValue().toString());
        }
        return result;
    }

    /**
     * Get a copy of the MDC map. This is a relatively expensive operation.
     * If the informed context is null it falls back to the thread local context map.
     *
     * @return a copy of the map
     */
    public Map<String, Object> copyObject(Context vertxContext) {
        return new HashMap<>(contextualDataMap(vertxContext));
    }

    /**
     * Clear the current MDC map.
     * Tries to use the current Vert.x Context, if the context is non-existent
     * meaning that it was called out of a Vert.x thread it will fall back to
     * the thread local context map.
     */
    @Override
    public void clear() {
        clear(getContext());
    }

    /**
     * Clear the current MDC map.
     * If the informed context is null it falls back to the thread local context map.
     */
    public void clear(Context vertxContext) {
        contextualDataMap(vertxContext).clear();
    }

    /**
     * Gets the current duplicated context or a new duplicated context if a Vert.x Context exists. Multiple invocations
     * of this method may return the same or different context. If the current context is a duplicate one, multiple
     * invocations always return the same context. If the current context is not duplicated, a new instance is returned
     * with each method invocation.
     *
     * @return a duplicated Vert.x Context or null.
     */
    private Context getContext() {
        Context context = Vertx.currentContext();
        if (context != null) {
            Context dc = getOrCreateDuplicatedContext(context);
            setContextSafe(dc, true);
            return dc;
        }
        return null;
    }

    /**
     * Gets the current Contextual Data Map from the current Vert.x Context if it is not null or the default
     * ThreadLocal Data Map for use in non Vert.x Threads.
     *
     * @return the current Contextual Data Map.
     */
    @SuppressWarnings({ "unchecked" })
    private Map<String, Object> contextualDataMap(Context ctx) {
        if (ctx == null) {
            return inheritableThreadLocalMap.get();
        }

        ConcurrentMap<Object, Object> lcd = Objects.requireNonNull((ContextInternal) ctx).localContextData();
        return (ConcurrentMap<String, Object>) lcd.computeIfAbsent(VertxMDC.class.getName(),
                k -> new ConcurrentHashMap<String, Object>());
    }
}
