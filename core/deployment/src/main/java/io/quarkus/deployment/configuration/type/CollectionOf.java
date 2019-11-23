package io.quarkus.deployment.configuration.type;

import java.util.Objects;

/**
 *
 */
public final class CollectionOf extends ConverterType {
    private final ConverterType type;
    private final Class<?> collectionClass;
    private int hashCode;

    public CollectionOf(final ConverterType type, final Class<?> collectionClass) {
        this.type = type;
        this.collectionClass = collectionClass;
    }

    public ConverterType getElementType() {
        return type;
    }

    @Override
    public Class<?> getLeafType() {
        return type.getLeafType();
    }

    public Class<?> getCollectionClass() {
        return collectionClass;
    }

    @Override
    public int hashCode() {
        int hashCode = this.hashCode;
        if (hashCode == 0) {
            hashCode = Objects.hash(type, collectionClass, CollectionOf.class);
            if (hashCode == 0) {
                hashCode = 0x8000_0000;
            }
            this.hashCode = hashCode;
        }
        return hashCode;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof CollectionOf && equals((CollectionOf) obj);
    }

    public boolean equals(final CollectionOf obj) {
        return this == obj || obj != null && type.equals(obj.type);
    }
}
