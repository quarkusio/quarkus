package io.quarkus.funqy.runtime;

import java.util.Map;

public interface RequestContext {
    Object getProperty(String name);

    Map<String, Object> getProperties();

    void setProperty(String name, Object value);

    <T> T getContextData(Class<T> key);

    void setContextData(Class<?> key, Object value);

    Map<Class<?>, Object> getContextData();
}
