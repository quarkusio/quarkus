package io.quarkus.arc.impl;

import static io.quarkus.arc.impl.TypeCachePollutionUtils.asParameterizedType;
import static io.quarkus.arc.impl.TypeCachePollutionUtils.isParameterizedType;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

/**
 * This code was mainly copied from Weld codebase.
 *
 * @author Pete Muir and Weld contributors
 */
final class Types {

    private Types() {
    }

    static Type boxedType(Type type) {
        if (type instanceof Class<?>) {
            return boxedClass((Class<?>) type);
        } else {
            return type;
        }
    }

    static Class<?> boxedClass(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        } else if (type.equals(Boolean.TYPE)) {
            return Boolean.class;
        } else if (type.equals(Character.TYPE)) {
            return Character.class;
        } else if (type.equals(Byte.TYPE)) {
            return Byte.class;
        } else if (type.equals(Short.TYPE)) {
            return Short.class;
        } else if (type.equals(Integer.TYPE)) {
            return Integer.class;
        } else if (type.equals(Long.TYPE)) {
            return Long.class;
        } else if (type.equals(Float.TYPE)) {
            return Float.class;
        } else if (type.equals(Double.TYPE)) {
            return Double.class;
        } else if (type.equals(Void.TYPE)) {
            return Void.class;
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Determines whether the given type is an actual type. A type is considered actual if it is a raw type,
     * a parameterized type or an array type.
     *
     * @param type the given type
     * @return true if and only if the given type is an actual type
     */
    static boolean isActualType(Type type) {
        return (type instanceof Class<?>) || (type instanceof ParameterizedType) || (type instanceof GenericArrayType);
    }

    /**
     * Determines whether the given type is an array type.
     *
     * @param type the given type
     * @return true if the given type is a subclass of java.lang.Class or implements GenericArrayType
     */
    static boolean isArray(Type type) {
        return (type instanceof GenericArrayType) || (type instanceof Class<?> && ((Class<?>) type).isArray());
    }

    /**
     * Determines the component type for a given array type.
     *
     * @param type the given array type
     * @return the component type of a given array type
     */
    public static Type getArrayComponentType(Type type) {
        if (type instanceof GenericArrayType) {
            return GenericArrayType.class.cast(type).getGenericComponentType();
        }
        if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            if (clazz.isArray()) {
                return clazz.getComponentType();
            }
        }
        throw new IllegalArgumentException("Not an array type " + type);
    }

    /**
     * Determines whether the given array only contains unbounded type variables or Object.class.
     *
     * @param types the given array of types
     * @return true if and only if the given array only contains unbounded type variables or Object.class
     */
    static boolean isArrayOfUnboundedTypeVariablesOrObjects(Type[] types) {
        for (Type type : types) {
            if (Object.class.equals(type)) {
                continue;
            }
            if (type instanceof TypeVariable<?>) {
                Type[] bounds = ((TypeVariable<?>) type).getBounds();
                if (bounds == null || bounds.length == 0 || (bounds.length == 1 && Object.class.equals(bounds[0]))) {
                    continue;
                }
            }
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    static <T> Class<T> getRawType(Type type) {
        if (type instanceof Class<?>) {
            return (Class<T>) type;
        }
        if (isParameterizedType(type)) {
            final ParameterizedType parameterizedType = asParameterizedType(type);
            if (parameterizedType.getRawType() instanceof Class<?>) {
                return (Class<T>) parameterizedType.getRawType();
            }
        }
        if (type instanceof TypeVariable<?>) {
            TypeVariable<?> variable = (TypeVariable<?>) type;
            Type[] bounds = variable.getBounds();
            return getBound(bounds);
        }
        if (type instanceof WildcardType) {
            WildcardType wildcard = (WildcardType) type;
            return getBound(wildcard.getUpperBounds());
        }
        if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            Class<?> rawType = getRawType(genericArrayType.getGenericComponentType());
            if (rawType != null) {
                return (Class<T>) Array.newInstance(rawType, 0).getClass();
            }
        }
        return null;
    }

    /**
     * Returns a canonical type for a given class.
     *
     * If the class is a raw type of a parameterized class, the matching {@link ParameterizedType} (with unresolved type
     * variables) is resolved.
     *
     * If the class is an array then the component type of the array is canonicalized
     *
     * Otherwise, the class is returned.
     *
     * @return
     */
    static Type getCanonicalType(Class<?> clazz) {
        if (clazz.isArray()) {
            Class<?> componentType = clazz.getComponentType();
            Type resolvedComponentType = getCanonicalType(componentType);
            if (componentType != resolvedComponentType) {
                // identity check intentional
                // a different identity means that we actually replaced the component Class with a ParameterizedType
                return new GenericArrayTypeImpl(resolvedComponentType);
            }
        }
        if (clazz.getTypeParameters().length > 0) {
            Type[] actualTypeParameters = clazz.getTypeParameters();
            return new ParameterizedTypeImpl(clazz, actualTypeParameters, clazz.getDeclaringClass());
        }
        return clazz;
    }

    public static Type getCanonicalType(Type type) {
        if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            return getCanonicalType(clazz);
        }
        return type;
    }

    static boolean isRawGenericType(Type type) {
        if (!(type instanceof Class<?>)) {
            return false;
        }
        Class<?> clazz = (Class<?>) type;
        if (clazz.isArray()) {
            Class<?> componentType = clazz.getComponentType();
            return isRawGenericType(componentType);
        }
        return clazz.getTypeParameters().length > 0;
    }

    static boolean containsTypeVariable(Type type) {
        type = getCanonicalType(type);
        if (type instanceof TypeVariable<?>) {
            return true;
        }
        if (isParameterizedType(type)) {
            ParameterizedType parameterizedType = asParameterizedType(type);
            for (Type t : parameterizedType.getActualTypeArguments()) {
                if (containsTypeVariable(t)) {
                    return true;
                }
            }
        }
        if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            return containsTypeVariable(genericArrayType.getGenericComponentType());
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> getBound(Type[] bounds) {
        if (bounds.length == 0) {
            return (Class<T>) Object.class;
        } else {
            return getRawType(bounds[0]);
        }
    }

}
