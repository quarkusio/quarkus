package io.quarkus.deployment.configuration.type;

import java.util.Objects;

/**
 *
 */
public final class LowerBoundCheckOf extends ConverterType {
    private final Class<?> lowerBound;
    private final ConverterType classConverterType;
    private int hashCode;

    public LowerBoundCheckOf(final Class<?> lowerBound, final ConverterType classConverterType) {
        this.lowerBound = lowerBound;
        this.classConverterType = classConverterType;
    }

    public Class<?> getLowerBound() {
        return lowerBound;
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
            hashCode = Objects.hash(classConverterType, lowerBound, LowerBoundCheckOf.class);
            if (hashCode == 0) {
                hashCode = 0x8000_0000;
            }
            this.hashCode = hashCode;
        }
        return hashCode;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof LowerBoundCheckOf && equals((LowerBoundCheckOf) obj);
    }

    public boolean equals(final LowerBoundCheckOf obj) {
        return obj == this || obj != null && lowerBound == obj.lowerBound && classConverterType.equals(obj.classConverterType);
    }
}
