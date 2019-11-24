package io.quarkus.deployment.configuration.type;

import java.util.Objects;

/**
 *
 */
public final class OptionalOf extends ConverterType {
    private final ConverterType type;
    private int hashCode;

    public OptionalOf(final ConverterType type) {
        this.type = type;
    }

    public ConverterType getNestedType() {
        return type;
    }

    @Override
    public int hashCode() {
        int hashCode = this.hashCode;
        if (hashCode == 0) {
            hashCode = Objects.hash(type, OptionalOf.class);
            if (hashCode == 0) {
                hashCode = 0x8000_0000;
            }
            this.hashCode = hashCode;
        }
        return hashCode;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof OptionalOf && equals((OptionalOf) obj);
    }

    public boolean equals(final OptionalOf obj) {
        return this == obj || obj != null && type.equals(obj.type);
    }

    public Class<?> getLeafType() {
        return type.getLeafType();
    }
}
