package io.quarkus.cli.commands;

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

    public Object getValue(String name) {
        return getValue(name, null);
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(String name, T defaultValue) {
        final Object value = values.getOrDefault(name, NOT_SET);
        if(value == NOT_SET) {
            return defaultValue;
        }
        if(value == null) {
            return null;
        }
        return (T) value;
    }

    public boolean getValue(String name, boolean defaultValue) {
        final Object value = getValue(name, null);
        if (value == null) {
            return defaultValue;
        }
        if (Boolean.class.equals(value.getClass())) {
            return ((Boolean) value).booleanValue();
        }
        return Boolean.parseBoolean(value.toString());
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
