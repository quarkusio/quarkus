package io.quarkus.deployment.configuration.type;

import java.util.Objects;

/**
 *
 */
public final class MinMaxValidated extends ConverterType {
    private final ConverterType type;
    private final String min;
    private final boolean minInclusive;
    private final String max;
    private final boolean maxInclusive;
    private int hashCode;

    public MinMaxValidated(final ConverterType type, final String min, final boolean minInclusive, final String max,
            final boolean maxInclusive) {
        this.type = type;
        this.min = min;
        this.minInclusive = minInclusive;
        this.max = max;
        this.maxInclusive = maxInclusive;
    }

    public ConverterType getNestedType() {
        return type;
    }

    public String getMin() {
        return min;
    }

    public boolean isMinInclusive() {
        return minInclusive;
    }

    public String getMax() {
        return max;
    }

    public boolean isMaxInclusive() {
        return maxInclusive;
    }

    @Override
    public Class<?> getLeafType() {
        return type.getLeafType();
    }

    @Override
    public int hashCode() {
        int hashCode = this.hashCode;
        if (hashCode == 0) {
            hashCode = Objects.hash(type, min, Boolean.valueOf(minInclusive), max, Boolean.valueOf(maxInclusive));
            if (hashCode == 0) {
                hashCode = 0x8000_0000;
            }
            this.hashCode = hashCode;
        }
        return hashCode;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof MinMaxValidated && equals((MinMaxValidated) obj);
    }

    public boolean equals(final MinMaxValidated obj) {
        return this == obj || obj != null && Objects.equals(type, obj.type) && Objects.equals(min, obj.min)
                && Objects.equals(max, obj.max) && maxInclusive == obj.maxInclusive && minInclusive == obj.minInclusive;
    }
}
