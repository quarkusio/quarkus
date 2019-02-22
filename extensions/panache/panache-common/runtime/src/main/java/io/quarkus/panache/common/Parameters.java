package io.quarkus.panache.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Parameters {
    private final Map<String, Object> values = new HashMap<>();

    public Parameters and(String name, Object value) {
        values.put(name, value);
        return this;
    }

    public Map<String, Object> map() {
        return Collections.unmodifiableMap(values);
    }

    public static Parameters with(String name, Object value) {
        return new Parameters().and(name, value);
    }
}
