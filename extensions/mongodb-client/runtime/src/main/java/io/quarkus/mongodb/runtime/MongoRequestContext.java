package io.quarkus.mongodb.runtime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import com.mongodb.RequestContext;

import io.opentelemetry.context.Context;

@SuppressWarnings("unchecked")
public class MongoRequestContext implements RequestContext {
    public static final String OTEL_CONTEXT_KEY = "otel.context.current";
    private final Map<Object, Object> valuesMap;

    public MongoRequestContext(Context currentContext) {
        valuesMap = new ConcurrentHashMap<>();
        if (currentContext != null) {
            valuesMap.put(OTEL_CONTEXT_KEY, currentContext);
        }
    }

    @Override
    public <T> T get(Object key) {
        return (T) valuesMap.get(key);
    }

    @Override
    public boolean hasKey(Object key) {
        return valuesMap.containsKey(key);
    }

    @Override
    public boolean isEmpty() {
        return valuesMap.isEmpty();
    }

    @Override
    public void put(Object key, Object value) {
        valuesMap.put(key, value);
    }

    @Override
    public void delete(Object key) {
        valuesMap.remove(key);
    }

    @Override
    public int size() {
        return valuesMap.size();
    }

    @Override
    public Stream<Map.Entry<Object, Object>> stream() {
        return valuesMap.entrySet().stream();
    }
}
