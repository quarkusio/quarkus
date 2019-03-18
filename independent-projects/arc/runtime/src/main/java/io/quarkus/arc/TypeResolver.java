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
import java.util.Map;

/**
 * This code was mainly copied from Weld codebase.
 */
class TypeResolver {

    private final Map<TypeVariable<?>, Type> resolvedTypeVariables;

    public TypeResolver(Map<TypeVariable<?>, Type> resolvedTypeVariables) {
        this.resolvedTypeVariables = resolvedTypeVariables;
    }

    /**
     * Resolves a given type variable. This is achieved by a lookup in the {@link #resolvedTypeVariables} map.
     */
    public Type resolveType(TypeVariable<?> variable) {
        Type resolvedType = this.resolvedTypeVariables.get(variable);
        if (resolvedType == null) {
            return variable; // we are not able to resolve
        }
        return resolvedType;
    }

    /**
     * Resolves a given parameterized type. If the parameterized type contains no type variables it is returned untouched.
     * Otherwise, a new {@link ParameterizedType} instance is returned in which each type variable is resolved using
     * {@link #resolveType(TypeVariable)}.
     */
    public Type resolveType(ParameterizedType type) {
        Type[] unresolvedTypeArguments = type.getActualTypeArguments();

        /*
         * Indicates whether we managed to resolve any of type arguments. If we did not then there is no need to create a new
         * ParameterizedType with the old parameters. Instead, we return the original type.
         */
        boolean modified = false;
        Type[] resolvedTypeArguments = new Type[unresolvedTypeArguments.length];

        for (int i = 0; i < unresolvedTypeArguments.length; i++) {
            Type resolvedType = unresolvedTypeArguments[i];
            if (resolvedType instanceof TypeVariable<?>) {
                resolvedType = resolveType((TypeVariable<?>) resolvedType);
            }
            if (resolvedType instanceof ParameterizedType) {
                resolvedType = resolveType((ParameterizedType) resolvedType);
            }
            resolvedTypeArguments[i] = resolvedType;
            // This identity check is intentional. A different identity indicates that the type argument was resolved within #resolveType().
            if (unresolvedTypeArguments[i] != resolvedType) {
                modified = true;
            }
        }

        if (modified) {
            return new ParameterizedTypeImpl(type.getRawType(), resolvedTypeArguments, type.getOwnerType());
        } else {
            return type;
        }
    }

    public Type resolveType(GenericArrayType type) {
        Type genericComponentType = type.getGenericComponentType();
        // try to resolve the type
        Type resolvedType = genericComponentType;
        if (genericComponentType instanceof TypeVariable<?>) {
            resolvedType = resolveType((TypeVariable<?>) genericComponentType);
        }
        if (genericComponentType instanceof ParameterizedType) {
            resolvedType = resolveType((ParameterizedType) genericComponentType);
        }
        if (genericComponentType instanceof GenericArrayType) {
            resolvedType = resolveType((GenericArrayType) genericComponentType);
        }
        /*
         * If the generic component type resolved to a class (e.g. String) we return [Ljava.lang.String; (the class representing
         * the
         * array) instead of GenericArrayType with String as its generic component type.
         */
        if (resolvedType instanceof Class<?>) {
            Class<?> componentClass = (Class<?>) resolvedType;
            return Array.newInstance(componentClass, 0).getClass();
        }
        /*
         * This identity check is intentional. If the identity is different it indicates that we succeeded in resolving the type
         * and a new GenericArrayType with resolved generic component type is returned. Otherwise, we were not able to resolve
         * the type and therefore we do not create a new GenericArrayType.
         */
        if (resolvedType == genericComponentType) {
            return type;
        } else {
            return new GenericArrayTypeImpl(resolvedType);
        }
    }

    public Type resolveType(Type type) {
        if (type instanceof ParameterizedType) {
            return resolveType((ParameterizedType) type);
        }
        if (type instanceof TypeVariable<?>) {
            return resolveType((TypeVariable<?>) type);
        }
        if (type instanceof GenericArrayType) {
            return resolveType((GenericArrayType) type);
        }
        return type;
    }

    public Map<TypeVariable<?>, Type> getResolvedTypeVariables() {
        return resolvedTypeVariables;
    }
}
