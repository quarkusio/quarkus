package io.quarkus.funqy.runtime;

import java.util.HashMap;
import java.util.Map;

public class RequestContextImpl implements RequestContext {
    protected Map<Class<?>, Object> contextData = new HashMap<>();
    protected Map<String, Object> properties = new HashMap<>();

    @Override
    public Object getProperty(String name) {
        return properties.get(name);
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public void setProperty(String name, Object value) {
        if (value == null) {
            properties.remove(name);
        } else {
            properties.put(name, value);
        }

    }

    @Override
    public <T> T getContextData(Class<T> key) {
        return (T) contextData.get(key);
    }

    @Override
    public void setContextData(Class<?> key, Object value) {
        if (value == null) {
            contextData.remove(key);
        } else {
            contextData.put(key, value);
        }

    }

    @Override
    public Map<Class<?>, Object> getContextData() {
        return contextData;
    }
}
