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
        params.put(name, Objects.requireNonNull(value));
        return self();
    }

    public THIS param(String name, byte value) {
        params.put(name, value);
        return self();
    }

    public THIS param(String name, byte[] value) {
        params.put(name, Objects.requireNonNull(value));
        return self();
    }

    public THIS param(String name, short value) {
        params.put(name, value);
        return self();
    }

    public THIS param(String name, short[] value) {
        params.put(name, Objects.requireNonNull(value));
        return self();
    }

    public THIS param(String name, int value) {
        params.put(name, value);
        return self();
    }

    public THIS param(String name, int[] value) {
        params.put(name, Objects.requireNonNull(value));
        return self();
    }

    public THIS param(String name, long value) {
        params.put(name, value);
        return self();
    }

    public THIS param(String name, long[] value) {
        params.put(name, Objects.requireNonNull(value));
        return self();
    }

    public THIS param(String name, float value) {
        params.put(name, value);
        return self();
    }

    public THIS param(String name, float[] value) {
        params.put(name, Objects.requireNonNull(value));
        return self();
    }

    public THIS param(String name, double value) {
        params.put(name, value);
        return self();
    }

    public THIS param(String name, double[] value) {
        params.put(name, Objects.requireNonNull(value));
        return self();
    }

    public THIS param(String name, char value) {
        params.put(name, value);
        return self();
    }

    public THIS param(String name, char[] value) {
        params.put(name, Objects.requireNonNull(value));
        return self();
    }

    public THIS param(String name, String value) {
        params.put(name, Objects.requireNonNull(value));
        return self();
    }

    public THIS param(String name, String[] value) {
        params.put(name, Objects.requireNonNull(value));
        return self();
    }

    public THIS param(String name, Enum<?> value) {
        params.put(name, Objects.requireNonNull(value));
        return self();
    }

    public THIS param(String name, Enum<?>[] value) {
        params.put(name, Objects.requireNonNull(value));
        return self();
    }

    public THIS param(String name, Class<?> value) {
        params.put(name, Objects.requireNonNull(value));
        return self();
    }

    public THIS param(String name, Class<?>[] value) {
        params.put(name, Objects.requireNonNull(value));
        return self();
    }

    public THIS param(String name, ClassInfo value) {
        params.put(name, Objects.requireNonNull(value));
        return self();
    }

    public THIS param(String name, ClassInfo[] value) {
        params.put(name, Objects.requireNonNull(value));
        return self();
    }

    public THIS param(String name, AnnotationInstance value) {
        params.put(name, Objects.requireNonNull(value));
        return self();
    }

    public THIS param(String name, AnnotationInstance[] value) {
        params.put(name, Objects.requireNonNull(value));
        return self();
    }

    public THIS param(String name, InvokerInfo value) {
        params.put(name, Objects.requireNonNull(value));
        return self();
    }

    public THIS param(String name, InvokerInfo[] value) {
        params.put(name, Objects.requireNonNull(value));
        return self();
    }
}
