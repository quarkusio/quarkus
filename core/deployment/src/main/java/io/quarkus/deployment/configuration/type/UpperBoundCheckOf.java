package io.quarkus.deployment.configuration.type;

import java.util.Objects;

/**
 *
 */
public final class UpperBoundCheckOf extends ConverterType {
    private final Class<?> upperBound;
    private final ConverterType classConverterType;
    private int hashCode;

    public UpperBoundCheckOf(final Class<?> upperBound, final ConverterType classConverterType) {
        this.upperBound = upperBound;
        this.classConverterType = classConverterType;
    }

    public Class<?> getUpperBound() {
        return upperBound;
    }

    public ConverterType getClassConverterType() {
        return classConverterType;
    }

    @Override
    public Class<?> getLeafType() {
        return classConverterType.getLeafType();
    }

    @Override
    public int hashCode() {
        int hashCode = this.hashCode;
        if (hashCode == 0) {
            hashCode = Objects.hash(classConverterType, upperBound, UpperBoundCheckOf.class);
            if (hashCode == 0) {
                hashCode = 0x8000_0000;
            }
            this.hashCode = hashCode;
        }
        return hashCode;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof UpperBoundCheckOf && equals((UpperBoundCheckOf) obj);
    }

    public boolean equals(final UpperBoundCheckOf obj) {
        return obj == this || obj != null && upperBound == obj.upperBound && classConverterType.equals(obj.classConverterType);
    }
}
