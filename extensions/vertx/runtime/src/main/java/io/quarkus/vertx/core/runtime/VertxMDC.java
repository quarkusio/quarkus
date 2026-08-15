package io.quarkus.vertx.core.runtime;

import static io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle.setContextSafe;
import static io.smallrye.common.vertx.VertxContext.getOrCreateDuplicatedContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logmanager.MDCProvider;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.spi.context.storage.ContextLocal;

public enum VertxMDC implements MDCProvider {
    INSTANCE;

    public static final ContextLocal<ConcurrentHashMap<String, Object>> MDC_LOCAL = VertxMDCServiceProvider.MDC_LOCAL;

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
     * <p>
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
     * <p>
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
     * <p>
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
     * <p>
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
     * Sets several entries.
     * <p>
     * Tries to use the current Vert.x Context, if the context is non-existent
     * meaning that it was called out of a Vert.x thread it will fall back to
     * the thread local context map.
     * <p>
     * Will look up the contextual data map just once.
     *
     * @param map contents to add
     * @param vertxContext
     */
    public void putAll(Map<String, Object> map, Context vertxContext) {
        Objects.requireNonNull(map);
        contextualDataMap(vertxContext).putAll(map);
    }

    /**
     * Removes a key.
     * <p>
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
     * <p>
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
     * <p>
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
     * <p>
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
     * Determine whether the current MDC map is empty.
     *
     * @return {@code true} if there are no bound MDC values, or {@code false} otherwise
     */
    public boolean isEmpty() {
        return contextualDataMap(getContext()).isEmpty();
    }

    /**
     * Determine whether the current MDC map is empty on the provided context
     *
     * @param vertxContext
     * @return
     */
    public boolean isEmpty(Context vertxContext) {
        return contextualDataMap(vertxContext).isEmpty();
    }

    public Set<Map.Entry<String, Object>> getEntrySet() {
        return new HashSet<>(contextualDataMap(getContext()).entrySet());
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
     * Clear the contents of the current MDC map.
     * Tries to use the current Vert.x Context, if the context is non-existent
     * meaning that it was called out of a Vert.x thread it will fall back to
     * the thread local context map.
     */
    @Override
    public void clear() {
        clear(getContext());
    }

    /**
     * Clears the contents of the current MDC map in the Context.
     * If the informed context is null it falls back to the thread local context map.
     */
    public void clear(Context vertxContext) {
        contextualDataMap(vertxContext).clear();
    }

    /**
     * Removes the specified MDC keys from the context's MDC map in place.
     * Non-discard keys are preserved. This method is safe for concurrent use
     * when multiple threads share the same Vert.x context.
     *
     * @param vertxContext the Vert.x context
     * @param discardMdcKeys keys to remove from the MDC
     */
    public void reinitializeVertxMdc(Context vertxContext, Set<String> discardMdcKeys) {
        if (vertxContext == null || discardMdcKeys == null || discardMdcKeys.isEmpty()) {
            return;
        }
        ConcurrentHashMap<String, Object> data = vertxContext.getLocal(MDC_LOCAL);
        if (data == null) {
            return;
        }
        discardMdcKeys.forEach(data::remove);
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
     * Gets the current MDC map from the current Vert.x Context if it is not null or the default
     * ThreadLocal Data Map for use in non Vert.x Threads.
     *
     * @return the current Contextual Data Map.
     */
    private Map<String, Object> contextualDataMap(Context ctx) {
        if (ctx == null) {
            return inheritableThreadLocalMap.get();
        }

        return ctx.getLocal(MDC_LOCAL, ConcurrentHashMap::new);
    }
}
