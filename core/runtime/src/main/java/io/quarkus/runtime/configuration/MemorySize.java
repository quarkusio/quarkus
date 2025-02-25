package io.quarkus.runtime.configuration;

import java.math.BigInteger;
import java.util.Objects;

/**
 * A type representing data sizes.
 */
public final class MemorySize {
    private final BigInteger value;

    public MemorySize(BigInteger value) {
        this.value = value;
    }

    /**
     * @return {@link Long} - the size of memory in bytes
     */
    public long asLongValue() {
        return value.longValueExact();
    }

    /**
     * @return {@link BigInteger} - the value
     */
    public BigInteger asBigInteger() {
        return value;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        MemorySize that = (MemorySize) object;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
