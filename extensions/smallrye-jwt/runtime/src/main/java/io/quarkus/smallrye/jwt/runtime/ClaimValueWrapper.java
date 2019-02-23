package io.quarkus.smallrye.jwt.runtime;

import org.eclipse.microprofile.jwt.ClaimValue;

/**
 * An implementation of the ClaimValue interface
 *
 * @param <T> the claim value type
 */
public class ClaimValueWrapper<T> implements ClaimValue<T> {
    private String name;

    private T value;

    public ClaimValueWrapper(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format("ClaimValueWrapper[@%s], name=%s, value[%s]=%s", Integer.toHexString(hashCode()),
                name, value.getClass(), value);
    }
}
