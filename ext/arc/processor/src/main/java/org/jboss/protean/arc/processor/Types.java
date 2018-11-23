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

package org.jboss.protean.arc.processor;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.jboss.jandex.TypeVariable;
import org.jboss.jandex.WildcardType;
import org.jboss.protean.arc.GenericArrayTypeImpl;
import org.jboss.protean.arc.ParameterizedTypeImpl;
import org.jboss.protean.arc.TypeVariableImpl;
import org.jboss.protean.arc.WildcardTypeImpl;
import org.jboss.protean.gizmo.BytecodeCreator;
import org.jboss.protean.gizmo.MethodDescriptor;
import org.jboss.protean.gizmo.ResultHandle;

/**
 *
 * @author Martin Kouba
 */
final class Types {

    private static final Type OBJECT_TYPE = Type.create(DotNames.OBJECT, Kind.CLASS);

    private Types() {
    }

    static ResultHandle getTypeHandle(BytecodeCreator creator, Type type) {
        if (Kind.CLASS.equals(type.kind())) {
            return creator.loadClass(type.asClassType().name().toString());
        } else if (Kind.TYPE_VARIABLE.equals(type.kind())) {
            // E.g. T -> new TypeVariableImpl("T")
            TypeVariable typeVariable = type.asTypeVariable();
            ResultHandle boundsHandle;
            List<Type> bounds = typeVariable.bounds();
            if (bounds.isEmpty()) {
                boundsHandle = creator.newArray(java.lang.reflect.Type.class, creator.load(0));
            } else {
                boundsHandle = creator.newArray(java.lang.reflect.Type.class, creator.load(bounds.size()));
                for (int i = 0; i < bounds.size(); i++) {
                    creator.writeArrayValue(boundsHandle, i, getTypeHandle(creator, bounds.get(i)));
                }
            }
            return creator.newInstance(MethodDescriptor.ofConstructor(TypeVariableImpl.class, String.class, java.lang.reflect.Type[].class),
                    creator.load(typeVariable.identifier()), boundsHandle);

        } else if (Kind.PARAMETERIZED_TYPE.equals(type.kind())) {
            // E.g. List<String> -> new ParameterizedTypeImpl(List.class, String.class)
            ParameterizedType parameterizedType = type.asParameterizedType();

            List<Type> arguments = parameterizedType.arguments();
            ResultHandle typeArgsHandle = creator.newArray(java.lang.reflect.Type.class, creator.load(arguments.size()));
            for (int i = 0; i < arguments.size(); i++) {
                creator.writeArrayValue(typeArgsHandle, i, getTypeHandle(creator, arguments.get(i)));
            }
            return creator.newInstance(
                    MethodDescriptor.ofConstructor(ParameterizedTypeImpl.class, java.lang.reflect.Type.class, java.lang.reflect.Type[].class),
                    creator.loadClass(parameterizedType.name().toString()), typeArgsHandle);

        } else if (Kind.ARRAY.equals(type.kind())) {
            Type componentType = type.asArrayType().component();
            // E.g. String[] -> new GenericArrayTypeImpl(String.class)
            return creator.newInstance(MethodDescriptor.ofConstructor(GenericArrayTypeImpl.class, java.lang.reflect.Type.class),
                    getTypeHandle(creator, componentType));

        } else if (Kind.WILDCARD_TYPE.equals(type.kind())) {
            // E.g. ? extends Number -> WildcardTypeImpl.withUpperBound(Number.class)
            WildcardType wildcardType = type.asWildcardType();

            if (wildcardType.superBound() == null) {
                return creator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(WildcardTypeImpl.class, "withUpperBound", java.lang.reflect.WildcardType.class, java.lang.reflect.Type.class),
                        getTypeHandle(creator, wildcardType.extendsBound()));
            } else {
                return creator.invokeStaticMethod(
                        MethodDescriptor.ofMethod(WildcardTypeImpl.class, "withLowerBound", java.lang.reflect.WildcardType.class, java.lang.reflect.Type.class),
                        getTypeHandle(creator, wildcardType.superBound()));
            }
        } else if (Kind.PRIMITIVE.equals(type.kind())) {
            switch (type.asPrimitiveType().primitive()) {
                case INT:
                    return creator.loadClass(int.class);
                case LONG:
                    return creator.loadClass(long.class);
                case BOOLEAN:
                    return creator.loadClass(boolean.class);
                case BYTE:
                    return creator.loadClass(byte.class);
                case CHAR:
                    return creator.loadClass(char.class);
                case DOUBLE:
                    return creator.loadClass(double.class);
                case FLOAT:
                    return creator.loadClass(float.class);
                case SHORT:
                    return creator.loadClass(short.class);
                default:
                    throw new IllegalArgumentException("Unsupported primitive type: " + type);
            }
        } else {
            throw new IllegalArgumentException("Unsupported bean type: " + type.kind() + ", " + type);
        }
    }

    static Type getProviderType(ClassInfo classInfo) {
        // TODO hack
        List<TypeVariable> typeParameters = classInfo.typeParameters();
        if (!typeParameters.isEmpty()) {
            return ParameterizedType.create(classInfo.name(), typeParameters.toArray(new Type[] {}), null);
        } else {
            return Type.create(classInfo.name(), Kind.CLASS);
        }
    }

    static Set<Type> getTypeClosure(MethodInfo producerMethod, BeanDeployment beanDeployment) {
        Type returnType = producerMethod.returnType();
        if (returnType.kind() == Kind.PRIMITIVE) {
            Set<Type> types = new HashSet<>();
            types.add(returnType);
            types.add(OBJECT_TYPE);
            return types;
        }
        ClassInfo returnTypeClassInfo = beanDeployment.getIndex().getClassByName(returnType.name());
        if (returnTypeClassInfo == null) {
            throw new IllegalArgumentException("Producer method return type not found in index: " + producerMethod.returnType().name());
        }
        if (Kind.CLASS.equals(returnType.kind())) {
            return getTypeClosure(returnTypeClassInfo, Collections.emptyMap(), beanDeployment);
        } else if (Kind.PARAMETERIZED_TYPE.equals(returnType.kind())) {
            return getTypeClosure(returnTypeClassInfo,
                    buildResolvedMap(returnType.asParameterizedType().arguments(), returnTypeClassInfo.typeParameters(), Collections.emptyMap()),
                    beanDeployment);
        } else {
            throw new IllegalArgumentException("Unsupported return type");
        }
    }

    static Set<Type> getTypeClosure(FieldInfo producerField, BeanDeployment beanDeployment) {
        Type fieldType = producerField.type();
        if (fieldType.kind() == Kind.PRIMITIVE) {
            Set<Type> types = new HashSet<>();
            types.add(fieldType);
            types.add(OBJECT_TYPE);
            return types;
        }
        ClassInfo fieldClassInfo = beanDeployment.getIndex().getClassByName(producerField.type().name());
        if (fieldClassInfo == null) {
            throw new IllegalArgumentException("Producer field type not found in index: " + producerField.type().name());
        }
        if (Kind.CLASS.equals(fieldType.kind())) {
            return getTypeClosure(fieldClassInfo, Collections.emptyMap(), beanDeployment);
        } else if (Kind.PARAMETERIZED_TYPE.equals(fieldType.kind())) {
            return getTypeClosure(fieldClassInfo,
                    buildResolvedMap(fieldType.asParameterizedType().arguments(), fieldClassInfo.typeParameters(), Collections.emptyMap()), beanDeployment);
        } else {
            throw new IllegalArgumentException("Unsupported return type");
        }
    }

    static Set<Type> getTypeClosure(ClassInfo classInfo, Map<TypeVariable, Type> resolvedTypeParameters, BeanDeployment beanDeployment) {
        Set<Type> types = new HashSet<>();
        List<TypeVariable> typeParameters = classInfo.typeParameters();
        if (!typeParameters.isEmpty()) {
            // Canonical ParameterizedType with unresolved type variables
            Type[] typeParams = new Type[typeParameters.size()];
            for (int i = 0; i < typeParameters.size(); i++) {
                TypeVariable paramType = typeParameters.get(i);
                Type resolvedType = resolvedTypeParameters.get(paramType);
                if (resolvedType == null) {
                    resolvedType = paramType.bounds().get(0);
                }
                typeParams[i] = resolvedType;
            }
            types.add(ParameterizedType.create(classInfo.name(), typeParams, null));
        } else {
            types.add(Type.create(classInfo.name(), Kind.CLASS));
        }
        // Interfaces
        for (Type interfaceType : classInfo.interfaceTypes()) {
            ClassInfo interfaceClassInfo = beanDeployment.getIndex().getClassByName(interfaceType.name());
            if (interfaceClassInfo != null) {
                Map<TypeVariable, Type> resolved = Collections.emptyMap();
                if (Kind.PARAMETERIZED_TYPE.equals(interfaceType.kind())) {
                    resolved = buildResolvedMap(interfaceType.asParameterizedType().arguments(), interfaceClassInfo.typeParameters(), resolvedTypeParameters);
                }
                types.addAll(getTypeClosure(interfaceClassInfo, resolved, beanDeployment));
            }
        }
        // Superclass
        if (classInfo.superClassType() != null) {
            ClassInfo superClassInfo = beanDeployment.getIndex().getClassByName(classInfo.superName());
            if (superClassInfo != null) {
                Map<TypeVariable, Type> resolved = Collections.emptyMap();
                if (Kind.PARAMETERIZED_TYPE.equals(classInfo.superClassType().kind())) {
                    resolved = buildResolvedMap(classInfo.superClassType().asParameterizedType().arguments(), superClassInfo.typeParameters(),
                            resolvedTypeParameters);
                }
                types.addAll(getTypeClosure(superClassInfo, resolved, beanDeployment));
            }
        }

        // Bean types restriction
        AnnotationInstance typed = classInfo.classAnnotations().stream().filter(a -> a.name().equals(DotNames.TYPED)).findFirst().orElse(null);
        if (typed != null) {
            Set<DotName> typedClasses = new HashSet<>();
            for (Type type : typed.value().asClassArray()) {
                typedClasses.add(type.name());
            }
            for (Iterator<Type> iterator = types.iterator(); iterator.hasNext();) {
                if (!typedClasses.contains(iterator.next().name())) {
                    iterator.remove();
                }
            }
        }
        return types;
    }

    static Map<TypeVariable, Type> buildResolvedMap(List<Type> resolvedTypeVariables, List<TypeVariable> typeVariables,
            Map<TypeVariable, Type> resolvedTypeParameters) {
        Map<TypeVariable, Type> resolvedMap = new HashMap<>();
        for (int i = 0; i < resolvedTypeVariables.size(); i++) {
            Type resolvedTypeVariable = resolvedTypeVariables.get(i);
            Type resolvedTypeParam = Kind.TYPE_VARIABLE.equals(resolvedTypeVariable.kind())
                    ? resolvedTypeParameters.getOrDefault(resolvedTypeVariable, resolvedTypeVariable)
                    : resolvedTypeVariable;
            resolvedMap.put(typeVariables.get(i), resolvedTypeParam);
        }
        return resolvedMap;
    }

    static String convertNested(DotName name) {
        return convertNested(name.toString());
    }

    static String convertNested(String name) {
        return name.replace('$', '.');
    }

    static String getPackageName(String className) {
        className = className.replace('/', '.');
        return className.contains(".") ? className.substring(0, className.lastIndexOf(".")) : "";
    }

    static String getSimpleName(String className) {
        return className.contains(".") ? className.substring(className.lastIndexOf(".") + 1, className.length()) : className;
    }

}
