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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ArrayType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;
import org.jboss.jandex.WildcardType;

final class Methods {

    private static final List<String> IGNORED_METHODS = initIgnoredMethods();

    private static List<String> initIgnoredMethods() {
        List<String> ignored = new ArrayList<>();
        ignored.add("<init>");
        ignored.add("<clinit>");
        return ignored;
    }

    private Methods() {
    }

    static void addDelegatingMethods(IndexView index, ClassInfo classInfo, Map<TypeVariable, Type> resolvedTypeParameters,
            Map<Methods.MethodKey, MethodInfo> methods) {
        // TODO support interfaces default methods
        if (classInfo != null) {
            for (MethodInfo method : classInfo.methods()) {
                if (skipForClientProxy(method)) {
                    continue;
                }
                methods.computeIfAbsent(new Methods.MethodKey(method), key -> {
                    // If parameterized try to resolve the type variables
                    Type returnType = resolveType(key.method.returnType(), resolvedTypeParameters);
                    Type[] params = new Type[key.method.parameters().size()];
                    for (int i = 0; i < params.length; i++) {
                        params[i] = resolveType(key.method.parameters().get(i), resolvedTypeParameters);
                    }
                    List<TypeVariable> typeVariables = key.method.typeParameters();
                    return MethodInfo.create(classInfo, key.method.name(), params, returnType, key.method.flags(), typeVariables.toArray(new TypeVariable[] {}),
                            key.method.exceptions().toArray(Type.EMPTY_ARRAY));
                });
            }
            // Interfaces
            for (Type interfaceType : classInfo.interfaceTypes()) {
                ClassInfo interfaceClassInfo = index.getClassByName(interfaceType.name());
                if (interfaceClassInfo != null) {
                    Map<TypeVariable, Type> resolved = Collections.emptyMap();
                    if (org.jboss.jandex.Type.Kind.PARAMETERIZED_TYPE.equals(interfaceType.kind())) {
                        resolved = Types.buildResolvedMap(interfaceType.asParameterizedType().arguments(), interfaceClassInfo.typeParameters(),
                                resolvedTypeParameters);
                    }
                    addDelegatingMethods(index, interfaceClassInfo, resolved, methods);
                }
            }
            if (classInfo.superClassType() != null) {
                ClassInfo superClassInfo = index.getClassByName(classInfo.superName());
                if (superClassInfo != null) {
                    Map<TypeVariable, Type> resolved = Collections.emptyMap();
                    if (org.jboss.jandex.Type.Kind.PARAMETERIZED_TYPE.equals(classInfo.superClassType().kind())) {
                        resolved = Types.buildResolvedMap(classInfo.superClassType().asParameterizedType().arguments(), superClassInfo.typeParameters(),
                                resolvedTypeParameters);
                    }
                    addDelegatingMethods(index, superClassInfo, resolved, methods);
                }
            }
        }
    }

    private static boolean skipForClientProxy(MethodInfo method) {
        if (Modifier.isStatic(method.flags()) || Modifier.isFinal(method.flags()) || Modifier.isPrivate(method.flags())) {
            return true;
        }
        if (IGNORED_METHODS.contains(method.name())) {
            return true;
        }
        if (method.declaringClass().name().equals(DotNames.OBJECT)) {
            return true;
        }
        return false;
    }

    static void addInterceptedMethodCandidates(BeanDeployment beanDeployment, ClassInfo classInfo, Map<MethodKey, Set<AnnotationInstance>> candidates,
            List<AnnotationInstance> classLevelBindings) {
        for (MethodInfo method : classInfo.methods()) {
            if (skipForSubclass(method)) {
                continue;
            }
            Collection<AnnotationInstance> methodAnnnotations = beanDeployment.getAnnotations(method);
            List<AnnotationInstance> methodLevelBindings = methodAnnnotations.stream()
                    .filter(a -> beanDeployment.getInterceptorBinding(a.name()) != null)
                    .collect(Collectors.toList());
            Set<AnnotationInstance> merged = new HashSet<>();
            merged.addAll(methodLevelBindings);
            for (AnnotationInstance classLevelBinding : classLevelBindings) {
                if (methodLevelBindings.stream().noneMatch(a -> classLevelBinding.name().equals(a.name()))) {
                    merged.add(classLevelBinding);
                }
            }
            if (!merged.isEmpty()) {
                candidates.computeIfAbsent(new Methods.MethodKey(method), key -> merged);
            }
        }
        if (classInfo.superClassType() != null) {
            ClassInfo superClassInfo = beanDeployment.getIndex().getClassByName(classInfo.superName());
            if (superClassInfo != null) {
                addInterceptedMethodCandidates(beanDeployment, superClassInfo, candidates, classLevelBindings);
            }
        }
    }

    private static boolean skipForSubclass(MethodInfo method) {
        if (Modifier.isStatic(method.flags()) || Modifier.isFinal(method.flags())) {
            return true;
        }
        if (IGNORED_METHODS.contains(method.name())) {
            return true;
        }
        if (method.declaringClass().name().equals(DotNames.OBJECT)) {
            return true;
        }
        return false;
    }

    static Type resolveType(Type type, Map<TypeVariable, Type> resolvedTypeParameters) {
        switch (type.kind()) {
            case CLASS:
            case PRIMITIVE:
            case VOID:
                return type;
            case TYPE_VARIABLE:
                // TODO bounds
                return resolvedTypeParameters.getOrDefault(type.asTypeVariable(), type);
            case PARAMETERIZED_TYPE:
                ParameterizedType parameterizedType = type.asParameterizedType();
                Type[] args = new Type[parameterizedType.arguments().size()];
                for (int i = 0; i < args.length; i++) {
                    args[i] = resolveType(parameterizedType.arguments().get(i), resolvedTypeParameters);
                }
                return ParameterizedType.create(parameterizedType.name(), args, null);
            case WILDCARD_TYPE:
                WildcardType wildcardType = type.asWildcardType();
                return WildcardType.create(
                        resolveType(wildcardType.superBound() != null ? wildcardType.superBound() : wildcardType.extendsBound(), resolvedTypeParameters),
                        wildcardType.superBound() == null);
            case ARRAY:
                ArrayType arrayType = type.asArrayType();
                return ArrayType.create(resolveType(arrayType.component(), resolvedTypeParameters), arrayType.dimensions());
            default:
                throw new IllegalArgumentException("Unsupported type to resolve: " + type);
        }
    }

    static class MethodKey {

        final MethodInfo method;

        public MethodKey(MethodInfo method) {
            this.method = method;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((method.name() == null) ? 0 : method.name().hashCode());
            result = prime * result + ((method.parameters() == null) ? 0 : method.parameters().hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof MethodKey)) {
                return false;
            }
            MethodKey other = (MethodKey) obj;
            if (!method.name().equals(other.method.name())) {
                return false;
            }
            // FIXME this does not handle generics!!!
            if (!method.parameters().equals(other.method.parameters())) {
                return false;
            }
            return true;
        }

    }

}
