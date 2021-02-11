package io.quarkus.runtime.configuration;

import java.math.BigInteger;

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
}
