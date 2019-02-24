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
 * Utility class that captures invariant Java assignability rules (used with generics).
 *
 * This class operates on all the possible Type subtypes: Class, ParameterizedType, TypeVariable, WildcardType, GenericArrayType. To make this class easier to
 * understand and maintain, there is a separate isAssignableFrom method for each combination of possible types. Each of these methods compares two type
 * instances and determines whether the first one is assignable from the other.
 *
 * Since Java wildcards are by definition covariant, this class does not operate on wildcards and instead delegates to {@link CovariantTypes}.
 *
 * TypeVariables are considered a specific unknown type restricted by the upper bound. No inference of type variables is performed.
 *
 * @author Jozef Hartinger
 *
 */
class InvariantTypes {

    private InvariantTypes() {
    }

    static boolean isAssignableFrom(Type type1, Type type2) {
        if (type1 instanceof WildcardType || type2 instanceof WildcardType) {
            // Wildcards are by definition covariant
            return CovariantTypes.isAssignableFrom(type1, type2);
        }
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
            if (type2 instanceof GenericArrayType) {
                return isAssignableFrom((Class<?>) type1, (GenericArrayType) type2);
            }
            throw unknownType(type2);
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
            if (type2 instanceof GenericArrayType) {
                return isAssignableFrom((ParameterizedType) type1, (GenericArrayType) type2);
            }
            throw unknownType(type2);
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
            if (type2 instanceof GenericArrayType) {
                return isAssignableFrom((TypeVariable<?>) type1, (GenericArrayType) type2);
            }
            throw unknownType(type2);
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
            if (type2 instanceof GenericArrayType) {
                return isAssignableFrom((GenericArrayType) type1, (GenericArrayType) type2);
            }
            throw unknownType(type2);
        }
        throw unknownType(type1);
    }

    static IllegalArgumentException unknownType(Type type) {
        return new IllegalArgumentException("Unknown type: " + type);
    }

    /*
     * Raw type
     */
    private static boolean isAssignableFrom(Class<?> type1, Class<?> type2) {
        return Types.boxedClass(type1).equals(Types.boxedClass(type2));
    }

    private static boolean isAssignableFrom(Class<?> type1, ParameterizedType type2) {
        return false;
    }

    private static boolean isAssignableFrom(Class<?> type1, TypeVariable<?> type2) {
        return false;
    }

    private static boolean isAssignableFrom(Class<?> type1, GenericArrayType type2) {
        return false;
    }

    /*
     * ParameterizedType
     */
    private static boolean isAssignableFrom(ParameterizedType type1, Class<?> type2) {
        return false;
    }

    private static boolean isAssignableFrom(ParameterizedType type1, ParameterizedType type2) {
        // first, raw types have to be equal
        if (!Types.getRawType(type1).equals(Types.getRawType(type2))) {
            return false;
        }
        final Type[] types1 = type1.getActualTypeArguments();
        final Type[] types2 = type2.getActualTypeArguments();
        if (types1.length != types2.length) {
            throw new IllegalArgumentException("Invalida argument combination: " + type1 + " and " + type2);
        }
        for (int i = 0; i < types1.length; i++) {
            if (!isAssignableFrom(types1[i], types2[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAssignableFrom(ParameterizedType type1, TypeVariable<?> type2) {
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

    private static boolean isAssignableFrom(TypeVariable<?> type1, TypeVariable<?> type2) {
        return type1.equals(type2);
    }

    private static boolean isAssignableFrom(TypeVariable<?> type1, GenericArrayType type2) {
        return false;
    }

    /*
     * GenericArrayType
     */
    private static boolean isAssignableFrom(GenericArrayType type1, Class<?> type2) {
        return false;
    }

    private static boolean isAssignableFrom(GenericArrayType type1, ParameterizedType type2) {
        return false;
    }

    private static boolean isAssignableFrom(GenericArrayType type1, TypeVariable<?> type2) {
        return false;
    }

    private static boolean isAssignableFrom(GenericArrayType type1, GenericArrayType type2) {
        return isAssignableFrom(type1.getGenericComponentType(), type2.getGenericComponentType());
    }
}
