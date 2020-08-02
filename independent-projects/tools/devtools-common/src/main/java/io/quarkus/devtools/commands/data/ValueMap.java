package io.quarkus.devtools.commands.data;

import java.util.HashMap;
import java.util.Map;

public class ValueMap<V extends ValueMap<V>> {

    static final String NOT_SET = "QUARKUS_VALUE_NOT_SET";

    protected final Map<String, Object> values;

    public ValueMap() {
        this(new HashMap<>());
    }

    public ValueMap(Map<String, Object> values) {
        this.values = values == null ? new HashMap<>() : values;
    }

    public ValueMap(ValueMap<?> values) {
        this(values.values);
    }

    public <T> T getValue(String name) {
        return getValue(name, null);
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(String name, T defaultValue) {
        final Object value = values.getOrDefault(name, NOT_SET);
        if (value == NOT_SET) {
            return defaultValue;
        }
        if (value == null) {
            return null;
        }
        return (T) value;
    }

    public String getStringValue(String name) {
        final Object value = getValue(name, null);
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return ((String) value);
        }
        throw new IllegalStateException("value for '" + name + "' must be a String");
    }

    public boolean getBooleanValue(String name) {
        final Object value = getValue(name, null);
        if (value == null) {
            throw new IllegalStateException("value for '" + name + "' must be defined");
        }

        if (Boolean.class.equals(value.getClass())) {
            return ((Boolean) value).booleanValue();
        }
        return Boolean.parseBoolean(value.toString());
    }

    public boolean getValue(String name, boolean defaultValue) {
        final Object value = getValue(name, null);
        if (value == null) {
            return defaultValue;
        }
        if (Boolean.class.equals(value.getClass())) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public boolean valueIs(String name, Object o) {
        final Object value = values.get(name);
        return o == null ? value == null : o.equals(value);
    }

    public boolean hasValue(String name) {
        return values.getOrDefault(name, NOT_SET) != NOT_SET;
    }

    @SuppressWarnings("unchecked")
    public V setValue(String name, Object value) {
        values.put(name, value);
        return (V) this;
    }

    public V setValue(String name, boolean value) {
        return setValue(name, Boolean.valueOf(value));
    }
}
