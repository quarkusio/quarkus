/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.arc;

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

    static boolean isActualType(Type type) {
        return (type instanceof Class<?>) || (type instanceof ParameterizedType) || (type instanceof GenericArrayType);
    }

    static boolean isArray(Type type) {
        return (type instanceof GenericArrayType) || (type instanceof Class<?> && ((Class<?>) type).isArray());
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
        if (type instanceof ParameterizedType) {
            if (((ParameterizedType) type).getRawType() instanceof Class<?>) {
                return (Class<T>) ((ParameterizedType) type).getRawType();
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
     * If the class is a raw type of a parameterized class, the matching {@link ParameterizedType} (with unresolved type variables) is resolved.
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
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
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
