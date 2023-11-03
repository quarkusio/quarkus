package io.quarkus.vertx.mdc.provider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.jboss.logmanager.MDCProvider;

/**
 * Class enabling Quarkus to instantiate a {@link MDCProvider}
 * and set a delegate during runtime initialization.
 *
 * While/when no delegate is set it serves as a thread local MDC.
 *
 * LateBoundMDCProvider is an implementation of the MDC Provider SPI
 * it will only be used/discovered if a provider configuration file
 * {@code META-INF/services/org.jboss.logmanager.MDCProvider } is created.
 */
@SuppressWarnings({ "unused" })
public class LateBoundMDCProvider implements MDCProvider {
    private static volatile MDCProvider delegate;

    private final InheritableThreadLocal<Map<String, Object>> threadLocalMap = new InheritableThreadLocal<>() {
        @Override
        protected Map<String, Object> childValue(Map<String, Object> parentValue) {
            if (parentValue == null) {
                return null;
            }
            return new HashMap<>(parentValue);
        }
    };

    /**
     * Set the actual {@link MDCProvider} to use as the delegate.
     *
     * @param delegate Properly constructed {@link MDCProvider}.
     */
    public synchronized static void setMDCProviderDelegate(MDCProvider delegate) {
        LateBoundMDCProvider.delegate = delegate;
    }

    @Override
    public String get(String key) {
        if (delegate == null) {
            Object value = getLocal(key);
            return value != null ? value.toString() : null;
        }
        return delegate.get(key);
    }

    @Override
    public Object getObject(String key) {
        if (delegate == null) {
            return getLocal(key);
        }
        return delegate.getObject(key);
    }

    @Override
    public String put(String key, String value) {
        if (delegate == null) {
            Object oldValue = putLocal(key, value);
            return oldValue != null ? oldValue.toString() : null;
        }
        return delegate.put(key, value);
    }

    @Override
    public Object putObject(String key, Object value) {
        if (delegate == null) {
            return putLocal(key, value);
        }
        return delegate.putObject(key, value);
    }

    @Override
    public String remove(String key) {
        if (delegate == null) {
            Object oldValue = removeLocal(key);
            return oldValue != null ? oldValue.toString() : null;
        }
        return delegate.remove(key);
    }

    @Override
    public Object removeObject(String key) {
        if (delegate == null) {
            return removeLocal(key);
        }
        return delegate.removeObject(key);
    }

    @Override
    public Map<String, String> copy() {
        if (delegate == null) {
            final HashMap<String, String> result = new HashMap<>();
            Map<String, Object> currentMap = threadLocalMap.get();
            if (currentMap != null) {
                for (Map.Entry<String, Object> entry : currentMap.entrySet()) {
                    result.put(entry.getKey(), entry.getValue().toString());
                }
            }
            return result;
        }
        return delegate.copy();
    }

    @Override
    public Map<String, Object> copyObject() {
        if (delegate == null) {
            Map<String, Object> currentMap = threadLocalMap.get();
            if (currentMap != null) {
                return new HashMap<>(currentMap);
            } else {
                return Collections.emptyMap();
            }
        }
        return delegate.copyObject();
    }

    @Override
    public boolean isEmpty() {
        if (delegate == null) {
            Map<String, Object> currentMap = threadLocalMap.get();
            return currentMap == null || currentMap.isEmpty();
        } else {
            return delegate.isEmpty();
        }
    }

    @Override
    public void clear() {
        if (delegate == null) {
            Map<String, Object> map = threadLocalMap.get();
            if (map != null) {
                map.clear();
                threadLocalMap.remove();
            }
        } else {
            delegate.clear();
        }
    }

    private Object putLocal(String key, Object value) {
        Objects.requireNonNull(key);
        Objects.requireNonNull(value);

        Map<String, Object> map = threadLocalMap.get();
        if (map == null) {
            map = new HashMap<>();
            threadLocalMap.set(map);
        }
        return map.put(key, value);
    }

    private Object getLocal(String key) {
        Objects.requireNonNull(key);

        Map<String, Object> map = threadLocalMap.get();
        if (map != null) {
            return map.get(key);
        }
        return null;
    }

    private Object removeLocal(String key) {
        Objects.requireNonNull(key);

        Map<String, Object> map = threadLocalMap.get();
        if (map != null) {
            return map.remove(key);
        }
        return null;
    }
}
