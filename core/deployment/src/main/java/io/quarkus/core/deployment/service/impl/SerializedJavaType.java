package io.quarkus.core.deployment.service.impl;

import io.smallrye.serial.Serialized;

/**
 * A {@link Serialized} representation of a {@code java.lang.reflect.Type} value.
 * <p>
 * Produced by {@link JavaTypeSerializer} for the 3 types that we need to handle.
 * Each inner class holds the decomposed field values needed to reconstruct the object.
 */
sealed abstract class SerializedJavaType extends Serialized {
    /**
     * {@link java.lang.reflect.ParameterizedType} decomposed into the generic class, type arguments
     * and possibly owner type. Reconstructed as {@link io.quarkus.runtime.types.ParameterizedTypeImpl}.
     */
    static final class OfParameterizedType extends SerializedJavaType {
        private final Serialized genericClass;
        private final Serialized[] typeArguments;
        private final Serialized ownerClass;

        OfParameterizedType(Serialized genericClass, Serialized[] typeArguments, Serialized ownerClass) {
            this.genericClass = genericClass;
            this.typeArguments = typeArguments;
            this.ownerClass = ownerClass;
        }

        public Serialized genericClass() {
            return genericClass;
        }

        public Serialized[] typeArguments() {
            return typeArguments;
        }

        public Serialized ownerClass() {
            return ownerClass;
        }
    }

    /**
     * {@link java.lang.reflect.WildcardType} decomposed into the kind (unbounded, upper bound, lower bound)
     * and the bound. Reconstructed as {@link io.quarkus.runtime.types.WildcardTypeImpl}.
     */
    static final class OfWildcardType extends SerializedJavaType {
        public static final int UNBOUNDED = 0;
        public static final int UPPER_BOUND = 1;
        public static final int LOWER_BOUND = 2;

        private final int kind;
        private final Serialized bound;

        OfWildcardType(int kind, Serialized bound) {
            if (kind < UNBOUNDED || kind > LOWER_BOUND) {
                throw new IllegalArgumentException("Unknown wildcard type kind: " + kind);
            }
            this.kind = kind;
            this.bound = bound;
        }

        public int kind() {
            return kind;
        }

        public Serialized bound() {
            return bound;
        }
    }

    /**
     * {@link java.lang.reflect.GenericArrayType} decomposed into the array component type.
     * Reconstructed as {@link io.quarkus.runtime.types.GenericArrayTypeImpl}.
     */
    static final class OfGenericArrayType extends SerializedJavaType {
        private final Serialized component;

        OfGenericArrayType(Serialized component) {
            this.component = component;
        }

        public Serialized component() {
            return component;
        }
    }
}
