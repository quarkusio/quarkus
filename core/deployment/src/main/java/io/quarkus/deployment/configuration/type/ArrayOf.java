package io.quarkus.deployment.configuration.type;

import java.lang.reflect.Array;
import java.util.Objects;

/**
 *
 */
public final class ArrayOf extends ConverterType {
    private final ConverterType type;
    private int hashCode;
    private Class<?> arrayType;

    public ArrayOf(final ConverterType type) {
        this.type = type;
    }

    public ConverterType getElementType() {
        return type;
    }

    @Override
    public Class<?> getLeafType() {
        return type.getLeafType();
    }

    public Class<?> getArrayType() {
        Class<?> arrayType = this.arrayType;
        if (arrayType == null) {
            this.arrayType = arrayType = Array.newInstance(getLeafType(), 0).getClass();
        }
        return arrayType;
    }

    @Override
    public int hashCode() {
        int hashCode = this.hashCode;
        if (hashCode == 0) {
            hashCode = Objects.hash(type, ArrayOf.class);
            if (hashCode == 0) {
                hashCode = 0x8000_0000;
            }
            this.hashCode = hashCode;
        }
        return hashCode;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof ArrayOf && equals((ArrayOf) obj);
    }

    public boolean equals(final ArrayOf obj) {
        return this == obj || obj != null && type.equals(obj.type);
    }
}
