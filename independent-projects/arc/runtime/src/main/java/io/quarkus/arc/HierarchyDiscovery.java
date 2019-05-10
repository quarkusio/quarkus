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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This code was mainly copied from Weld codebase.
 *
 * Utility class that discovers transitive type closure of a given type.
 *
 * @author Weld Community
 * @author Ales Justin
 * @author Marko Luksa
 * @author Jozef Hartinger
 */
class HierarchyDiscovery {

    private final Map<Class<?>, Type> types;
    private final Map<TypeVariable<?>, Type> resolvedTypeVariables;
    private final TypeResolver resolver;
    private final Set<Type> typeClosure;

    /**
     * Constructs a new {@link HierarchyDiscovery} instance.
     * 
     * @param type the type whose hierarchy will be discovered
     */
    HierarchyDiscovery(Type type) {
        this(type, new TypeResolver(new HashMap<TypeVariable<?>, Type>()));
    }

    HierarchyDiscovery(Type type, TypeResolver resolver) {
        this.types = new HashMap<Class<?>, Type>();
        this.resolver = resolver;
        this.resolvedTypeVariables = resolver.getResolvedTypeVariables();
        discoverTypes(type, false);
        this.typeClosure = new HashSet<>(types.values());
    }

    public Set<Type> getTypeClosure() {
        return typeClosure;
    }

    public Map<Class<?>, Type> getTypeMap() {
        return types;
    }

    protected void discoverTypes(Type type, boolean rawGeneric) {
        if (!rawGeneric) {
            rawGeneric = Types.isRawGenericType(type);
        }
        if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            this.types.put(clazz, clazz);
            discoverFromClass(clazz, rawGeneric);
        } else if (rawGeneric) {
            discoverTypes(Types.getRawType(type), rawGeneric);
        } else if (type instanceof GenericArrayType) {
            GenericArrayType arrayType = (GenericArrayType) type;
            Type genericComponentType = arrayType.getGenericComponentType();
            Class<?> rawComponentType = Types.getRawType(genericComponentType);
            if (rawComponentType != null) {
                Class<?> arrayClass = Array.newInstance(rawComponentType, 0).getClass();
                this.types.put(arrayClass, type);
                discoverFromClass(arrayClass, rawGeneric);
            }
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = (parameterizedType).getRawType();
            if (rawType instanceof Class<?>) {
                Class<?> clazz = (Class<?>) rawType;
                processTypeVariables(clazz.getTypeParameters(), parameterizedType.getActualTypeArguments());
                this.types.put(clazz, type);
                discoverFromClass(clazz, rawGeneric);
            }
        }
    }

    protected void discoverFromClass(Class<?> clazz, boolean rawGeneric) {
        if (clazz.getSuperclass() != null) {
            discoverTypes(processAndResolveType(clazz.getGenericSuperclass(), clazz.getSuperclass()), rawGeneric);
        }
        discoverInterfaces(clazz, rawGeneric);
    }

    protected void discoverInterfaces(Class<?> clazz, boolean rawGeneric) {
        Type[] genericInterfaces = clazz.getGenericInterfaces();
        Class<?>[] interfaces = clazz.getInterfaces();
        if (genericInterfaces.length == interfaces.length) {
            // this branch should execute every time!
            for (int i = 0; i < interfaces.length; i++) {
                discoverTypes(processAndResolveType(genericInterfaces[i], interfaces[i]), rawGeneric);
            }
        }
    }

    protected Type processAndResolveType(Type superclass, Class<?> rawSuperclass) {
        if (superclass instanceof ParameterizedType) {
            ParameterizedType parameterizedSuperclass = (ParameterizedType) superclass;
            processTypeVariables(rawSuperclass.getTypeParameters(), parameterizedSuperclass.getActualTypeArguments());
            return resolveType(parameterizedSuperclass);
        } else if (superclass instanceof Class<?>) {
            // this is not a parameterized type, nothing to resolve
            return superclass;
        }
        throw new RuntimeException("Unexpected type: " + superclass);
    }

    /*
     * Processing part. Every type variable is mapped to the actual type in the resolvedTypeVariablesMap. This map is used later
     * on for resolving types.
     */
    private void processTypeVariables(TypeVariable<?>[] variables, Type[] values) {
        for (int i = 0; i < variables.length; i++) {
            processTypeVariable(variables[i], values[i]);
        }
    }

    private void processTypeVariable(TypeVariable<?> variable, Type value) {
        if (value instanceof TypeVariable<?>) {
            value = resolveType(value);
        }
        this.resolvedTypeVariables.put(variable, value);
    }

    /*
     * Resolving part. Using resolvedTypeVariables map which was prepared in the processing part.
     */
    public Type resolveType(Type type) {
        if (type instanceof Class) {
            Type resolvedType = types.get(type);
            if (resolvedType != null) {
                return resolvedType;
            }
        }
        return resolver.resolveType(type);
    }

    public TypeResolver getResolver() {
        return resolver;
    }
}
