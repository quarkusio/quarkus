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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

/**
 * This code was mainly copied from Weld codebase.
 *
 * Utility class that captures standard covariant Java assignability rules.
 *
 * This class operates on all the possible Type subtypes: Class, ParameterizedType, TypeVariable, WildcardType, GenericArrayType.
 * To make this class easier to understand and maintain, there is a separate isAssignableFrom method for each combination
 * of possible types. Each of these methods compares two type instances and determines whether the first one is assignable from
 * the other.
 *
 * TypeVariables are considered a specific unknown type restricted by the upper bound. No inference of type variables is performed.
 *
 * @author Jozef Hartinger
 *
 */
class CovariantTypes {

    private CovariantTypes() {
    }

    static boolean isAssignableFromAtLeastOne(Type type1, Type[] types2) {
        for (Type type2 : types2) {
            if (isAssignableFrom(type1, type2)) {
                return true;
            }
        }
        return false;
    }

    static boolean isAssignableFrom(Type type1, Type type2) {
        if (type1 instanceof Class<?>) {
            if (type2 instanceof Class<?>) {
                return isAssignableFrom((Class<?>) type1, (Class<?>) type2);
            }
            if (type2 instanceof ParameterizedType) {
                return isAssignableFrom((Class<?>) type1, (ParameterizedType) type2);
            }
            if (type2 instanceof TypeVariable<?>) {
                return isAssignableFrom((Class<?>) type1, (TypeVariable<?>) type2);
            }
            if (type2 instanceof WildcardType) {
                return isAssignableFrom((Class<?>) type1, (WildcardType) type2);
            }
            if (type2 instanceof GenericArrayType) {
                return isAssignableFrom((Class<?>) type1, (GenericArrayType) type2);
            }
            throw InvariantTypes.unknownType(type2);
        }
        if (type1 instanceof ParameterizedType) {
            if (type2 instanceof Class<?>) {
                return isAssignableFrom((ParameterizedType) type1, (Class<?>) type2);
            }
            if (type2 instanceof ParameterizedType) {
                return isAssignableFrom((ParameterizedType) type1, (ParameterizedType) type2);
            }
            if (type2 instanceof TypeVariable<?>) {
                return isAssignableFrom((ParameterizedType) type1, (TypeVariable<?>) type2);
            }
            if (type2 instanceof WildcardType) {
                return isAssignableFrom((ParameterizedType) type1, (WildcardType) type2);
            }
            if (type2 instanceof GenericArrayType) {
                return isAssignableFrom((ParameterizedType) type1, (GenericArrayType) type2);
            }
            throw InvariantTypes.unknownType(type2);
        }
        if (type1 instanceof TypeVariable<?>) {
            if (type2 instanceof Class<?>) {
                return isAssignableFrom((TypeVariable<?>) type1, (Class<?>) type2);
            }
            if (type2 instanceof ParameterizedType) {
                return isAssignableFrom((TypeVariable<?>) type1, (ParameterizedType) type2);
            }
            if (type2 instanceof TypeVariable<?>) {
                return isAssignableFrom((TypeVariable<?>) type1, (TypeVariable<?>) type2);
            }
            if (type2 instanceof WildcardType) {
                return isAssignableFrom((TypeVariable<?>) type1, (WildcardType) type2);
            }
            if (type2 instanceof GenericArrayType) {
                return isAssignableFrom((TypeVariable<?>) type1, (GenericArrayType) type2);
            }
            throw InvariantTypes.unknownType(type2);
        }
        if (type1 instanceof WildcardType) {
            if (Types.isActualType(type2)) {
                return isAssignableFrom((WildcardType) type1, type2);
            }
            if (type2 instanceof TypeVariable<?>) {
                return isAssignableFrom((WildcardType) type1, (TypeVariable<?>) type2);
            }
            if (type2 instanceof WildcardType) {
                return isAssignableFrom((WildcardType) type1, (WildcardType) type2);
            }
            throw InvariantTypes.unknownType(type2);
        }
        if (type1 instanceof GenericArrayType) {
            if (type2 instanceof Class<?>) {
                return isAssignableFrom((GenericArrayType) type1, (Class<?>) type2);
            }
            if (type2 instanceof ParameterizedType) {
                return isAssignableFrom((GenericArrayType) type1, (ParameterizedType) type2);
            }
            if (type2 instanceof TypeVariable<?>) {
                return isAssignableFrom((GenericArrayType) type1, (TypeVariable<?>) type2);
            }
            if (type2 instanceof WildcardType) {
                return isAssignableFrom((GenericArrayType) type1, (WildcardType) type2);
            }
            if (type2 instanceof GenericArrayType) {
                return isAssignableFrom((GenericArrayType) type1, (GenericArrayType) type2);
            }
            throw InvariantTypes.unknownType(type2);
        }
        throw InvariantTypes.unknownType(type1);
    }

    /*
     * Raw type
     */
    private static boolean isAssignableFrom(Class<?> type1, Class<?> type2) {
        return Types.boxedClass(type1).isAssignableFrom(Types.boxedClass(type2));
    }

    private static boolean isAssignableFrom(Class<?> type1, ParameterizedType type2) {
        return type1.isAssignableFrom(Types.getRawType(type2));
    }

    private static boolean isAssignableFrom(Class<?> type1, TypeVariable<?> type2) {
        for (Type type : type2.getBounds()) {
            if (isAssignableFrom(type1, type)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAssignableFrom(Class<?> type1, WildcardType type2) {
        return false;
    }

    private static boolean isAssignableFrom(Class<?> type1, GenericArrayType type2) {
        return type1.equals(Object.class) || type1.isArray()
                && isAssignableFrom(type1.getComponentType(), Types.getRawType(type2.getGenericComponentType()));
    }

    /*
     * ParameterizedType
     */
    private static boolean isAssignableFrom(ParameterizedType type1, Class<?> type2) {
        Class<?> rawType1 = Types.getRawType(type1);

        // raw types have to be assignable
        if (!isAssignableFrom(rawType1, type2)) {
            return false;
        }
        // this is a raw type with missing type arguments
        if (!Types.getCanonicalType(type2).equals(type2)) {
            return true;
        }

        return matches(type1, new HierarchyDiscovery(type2));
    }

    private static boolean isAssignableFrom(ParameterizedType type1, ParameterizedType type2) {
        // first, raw types have to be assignable
        if (!isAssignableFrom(Types.getRawType(type1), Types.getRawType(type2))) {
            return false;
        }
        if (matches(type1, type2)) {
            return true;
        }
        return matches(type1, new HierarchyDiscovery(type2));
    }

    private static boolean matches(ParameterizedType type1, HierarchyDiscovery type2) {
        for (Type type : type2.getTypeClosure()) {
            if (type instanceof ParameterizedType && matches(type1, (ParameterizedType) type)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(ParameterizedType type1, ParameterizedType type2) {
        final Class<?> rawType1 = Types.getRawType(type1);
        final Class<?> rawType2 = Types.getRawType(type2);

        if (!rawType1.equals(rawType2)) {
            return false;
        }

        final Type[] types1 = type1.getActualTypeArguments();
        final Type[] types2 = type2.getActualTypeArguments();

        if (types1.length != types2.length) {
            throw new IllegalArgumentException("Invalida argument combination: " + type1 + " and " + type2);
        }
        for (int i = 0; i < type1.getActualTypeArguments().length; i++) {
            // Generics are invariant
            if (!InvariantTypes.isAssignableFrom(types1[i], types2[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAssignableFrom(ParameterizedType type1, TypeVariable<?> type2) {
        for (Type type : type2.getBounds()) {
            if (isAssignableFrom(type1, type)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAssignableFrom(ParameterizedType type1, WildcardType type2) {
        return false;
    }

    private static boolean isAssignableFrom(ParameterizedType type1, GenericArrayType type2) {
        return false;
    }

    /*
     * Type variable
     */
    private static boolean isAssignableFrom(TypeVariable<?> type1, Class<?> type2) {
        return false;
    }

    private static boolean isAssignableFrom(TypeVariable<?> type1, ParameterizedType type2) {
        return false;
    }

    /**
     * Returns <tt>true</tt> if <tt>type2</tt> is a "sub-variable" of <tt>type1</tt>, i.e. if they are equal or if
     * <tt>type2</tt> (transitively) extends <tt>type1</tt>.
     */
    private static boolean isAssignableFrom(TypeVariable<?> type1, TypeVariable<?> type2) {
        if (type1.equals(type2)) {
            return true;
        }
        // if a type variable extends another type variable, it cannot declare other bounds
        if (type2.getBounds()[0] instanceof TypeVariable<?>) {
            return isAssignableFrom(type1, (TypeVariable<?>) type2.getBounds()[0]);
        }
        return false;
    }

    private static boolean isAssignableFrom(TypeVariable<?> type1, WildcardType type2) {
        return false;
    }

    private static boolean isAssignableFrom(TypeVariable<?> type1, GenericArrayType type2) {
        return false;
    }

    /*
     * Wildcard
     */

    /**
     * This logic is shared for all actual types i.e. raw types, parameterized types and generic array types.
     */
    private static boolean isAssignableFrom(WildcardType type1, Type type2) {
        if (!isAssignableFrom(type1.getUpperBounds()[0], type2)) {
            return false;
        }
        if (type1.getLowerBounds().length > 0 && !isAssignableFrom(type2, type1.getLowerBounds()[0])) {
            return false;
        }
        return true;
    }

    private static boolean isAssignableFrom(WildcardType type1, TypeVariable<?> type2) {
        if (type1.getLowerBounds().length > 0) {
            return isAssignableFrom(type2, type1.getLowerBounds()[0]);
        }
        return isAssignableFrom(type1.getUpperBounds()[0], type2);
    }

    private static boolean isAssignableFrom(WildcardType type1, WildcardType type2) {
        if (!isAssignableFrom(type1.getUpperBounds()[0], type2.getUpperBounds()[0])) {
            return false;
        }

        if (type1.getLowerBounds().length > 0) {
            // the first type defines a lower bound
            if (type2.getLowerBounds().length > 0) {
                return isAssignableFrom(type2.getLowerBounds()[0], type1.getLowerBounds()[0]);
            } else {
                return false;
            }
        } else if (type2.getLowerBounds().length > 0) {
            // only the second type defines a lower bound
            return type1.getUpperBounds()[0].equals(Object.class);
        }
        return true;
    }

    /*
     * GenericArrayType
     */
    private static boolean isAssignableFrom(GenericArrayType type1, Class<?> type2) {
        return type2.isArray() && isAssignableFrom(Types.getRawType(type1.getGenericComponentType()), type2.getComponentType());
    }

    private static boolean isAssignableFrom(GenericArrayType type1, ParameterizedType type2) {
        return false;
    }

    private static boolean isAssignableFrom(GenericArrayType type1, TypeVariable<?> type2) {
        /*
         * JLS does not allow array types to be used as bounds of type variables
         */
        return false;
    }

    private static boolean isAssignableFrom(GenericArrayType type1, WildcardType type2) {
        return false;
    }

    private static boolean isAssignableFrom(GenericArrayType type1, GenericArrayType type2) {
        return isAssignableFrom(type1.getGenericComponentType(), type2.getGenericComponentType());
    }
}
