package io.quarkus.arc.processor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;

/**
 * Base class for configurators that accept a parameter map.
 * <p>
 * This construct is not thread-safe.
 */
public abstract class ConfiguratorBase<THIS extends ConfiguratorBase<THIS>> {

    protected final Map<String, Object> params = new HashMap<>();

    @SuppressWarnings("unchecked")
    protected THIS self() {
        return (THIS) this;
    }

    public THIS read(ConfiguratorBase<?> base) {
        params.clear();
        params.putAll(base.params);
        return self();
    }

    public THIS param(String name, boolean value) {
        params.put(name, value);
        return self();
    }

    public THIS param(String name, boolean[] value) {
        Objects.requireNonNull(value, "Parameter value can't be null");
        params.put(name, value);
        return self();
    }

    public THIS param(String name, byte value) {
        params.put(name, value);
        return self();
    }

    public THIS param(String name, byte[] value) {
        Objects.requireNonNull(value, "Parameter value can't be null");
        params.put(name, value);
        return self();
    }

    public THIS param(String name, short value) {
        params.put(name, value);
        return self();
    }

    public THIS param(String name, short[] value) {
        Objects.requireNonNull(value, "Parameter value can't be null");
        params.put(name, value);
        return self();
    }

    public THIS param(String name, int value) {
        params.put(name, value);
        return self();
    }

    public THIS param(String name, int[] value) {
        Objects.requireNonNull(value, "Parameter value can't be null");
        params.put(name, value);
        return self();
    }

    public THIS param(String name, long value) {
        params.put(name, value);
        return self();
    }

    public THIS param(String name, long[] value) {
        Objects.requireNonNull(value, "Parameter value can't be null");
        params.put(name, value);
        return self();
    }

    public THIS param(String name, float value) {
        params.put(name, value);
        return self();
    }

    public THIS param(String name, float[] value) {
        Objects.requireNonNull(value, "Parameter value can't be null");
        params.put(name, value);
        return self();
    }

    public THIS param(String name, double value) {
        params.put(name, value);
        return self();
    }

    public THIS param(String name, double[] value) {
        Objects.requireNonNull(value, "Parameter value can't be null");
        params.put(name, value);
        return self();
    }

    public THIS param(String name, char value) {
        params.put(name, value);
        return self();
    }

    public THIS param(String name, char[] value) {
        Objects.requireNonNull(value, "Parameter value can't be null");
        params.put(name, value);
        return self();
    }

    public THIS param(String name, String value) {
        Objects.requireNonNull(value, "Parameter value can't be null");
        params.put(name, value);
        return self();
    }

    public THIS param(String name, String[] value) {
        Objects.requireNonNull(value, "Parameter value can't be null");
        params.put(name, value);
        return self();
    }

    public THIS param(String name, Enum<?> value) {
        Objects.requireNonNull(value, "Parameter value can't be null");
        params.put(name, value);
        return self();
    }

    public THIS param(String name, Enum<?>[] value) {
        Objects.requireNonNull(value, "Parameter value can't be null");
        params.put(name, value);
        return self();
    }

    public THIS param(String name, Class<?> value) {
        Objects.requireNonNull(value, "Parameter value can't be null");
        params.put(name, value);
        return self();
    }

    public THIS param(String name, Class<?>[] value) {
        Objects.requireNonNull(value, "Parameter value can't be null");
        params.put(name, value);
        return self();
    }

    public THIS param(String name, ClassInfo value) {
        Objects.requireNonNull(value, "Parameter value can't be null");
        params.put(name, value);
        return self();
    }

    public THIS param(String name, ClassInfo[] value) {
        Objects.requireNonNull(value, "Parameter value can't be null");
        params.put(name, value);
        return self();
    }

    public THIS param(String name, AnnotationInstance value) {
        Objects.requireNonNull(value, "Parameter value can't be null");
        params.put(name, value);
        return self();
    }

    public THIS param(String name, AnnotationInstance[] value) {
        Objects.requireNonNull(value, "Parameter value can't be null");
        params.put(name, value);
        return self();
    }

}
