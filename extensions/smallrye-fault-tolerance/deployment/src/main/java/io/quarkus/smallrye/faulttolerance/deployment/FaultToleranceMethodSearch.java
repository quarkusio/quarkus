package io.quarkus.smallrye.faulttolerance.deployment;

import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.TypeVariable;
import org.jboss.jandex.VoidType;
import org.jboss.jandex.WildcardType;

import io.quarkus.arc.processor.AssignabilityCheck;
import io.quarkus.arc.processor.KotlinDotNames;

// copy of `io.smallrye.faulttolerance.config.SecurityActions` and translation from reflection to Jandex
// the original used the following license header:
//
// Copyright 2017 Red Hat, Inc, and individual contributors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
final class FaultToleranceMethodSearch {
    private final IndexView index;
    private final AssignabilityCheck assignability;

    FaultToleranceMethodSearch(IndexView index) {
        this.index = index;
        this.assignability = new AssignabilityCheck(index, null);
    }

    /**
     * Finds a fallback method for given guarded method. If the guarded method is present on given {@code beanClass}
     * and is actually declared by given {@code declaringClass} and has given {@code parameterTypes} and {@code returnType},
     * then a fallback method of given {@code name}, with parameter types and return type matching the parameter types
     * and return type of the guarded method, is searched for on the {@code beanClass} and its superclasses and
     * superinterfaces, according to the specification rules. Returns {@code null} if no matching fallback method exists.
     *
     * @param beanClass the class of the bean that has the guarded method
     * @param declaringClass the class that actually declares the guarded method (can be a supertype of bean class)
     * @param name name of the fallback method
     * @param parameterTypes parameter types of the guarded method
     * @param returnType return type of the guarded method
     * @return the fallback method or {@code null} if none exists
     */
    MethodInfo findFallbackMethod(ClassInfo beanClass, ClassInfo declaringClass,
            String name, Type[] parameterTypes, Type returnType) {

        Set<MethodInfo> result = findMethod(beanClass, declaringClass, name, parameterTypes, returnType, false);
        return result.isEmpty() ? null : result.iterator().next();
    }

    /**
     * Finds a set of fallback methods with exception parameter for given guarded method. If the guarded method
     * is present on given {@code beanClass} and is actually declared by given {@code declaringClass} and has given
     * {@code parameterTypes} and {@code returnType}, then fallback methods of given {@code name}, with parameter types
     * and return type matching the parameter types and return type of the guarded method, and with one additional
     * parameter assignable to {@code Throwable} at the end of parameter list, is searched for on the {@code beanClass}
     * and its superclasses and superinterfaces, according to the specification rules. Returns an empty set if no
     * matching fallback method exists.
     *
     * @param beanClass the class of the bean that has the guarded method
     * @param declaringClass the class that actually declares the guarded method (can be a supertype of bean class)
     * @param name name of the fallback method
     * @param parameterTypes parameter types of the guarded method
     * @param returnType return type of the guarded method
     * @return the fallback method or an empty set if none exists
     */
    Set<MethodInfo> findFallbackMethodsWithExceptionParameter(ClassInfo beanClass, ClassInfo declaringClass,
            String name, Type[] parameterTypes, Type returnType) {
        return findMethod(beanClass, declaringClass, name, parameterTypes, returnType, true);
    }

    /**
     * Finds a before retry method for given guarded method. If the guarded method is present on given {@code beanClass}
     * and is actually declared by given {@code declaringClass}, then a before retry method of given {@code name},
     * with no parameters and return type of {@code void}, is searched for on the {@code beanClass} and its superclasses and
     * superinterfaces, according to the specification rules. Returns {@code null} if no matching before retry method exists.
     *
     * @param beanClass the class of the bean that has the guarded method
     * @param declaringClass the class that actually declares the guarded method (can be a supertype of bean class)
     * @param name name of the before retry method
     * @return the before retry method or {@code null} if none exists
     */
    MethodInfo findBeforeRetryMethod(ClassInfo beanClass, ClassInfo declaringClass, String name) {
        Set<MethodInfo> result = findMethod(beanClass, declaringClass, name, new Type[0], VoidType.VOID, false);
        return result.isEmpty() ? null : result.iterator().next();
    }

    private Set<MethodInfo> findMethod(ClassInfo beanClass, ClassInfo declaringClass, String name,
            Type[] expectedParameterTypes, Type expectedReturnType, boolean expectedExceptionParameter) {

        Set<MethodInfo> result = new HashSet<>();

        TypeMapping expectedMapping = TypeMapping.createFor(beanClass, declaringClass, index);

        // if we find a matching method on the bean class or one of its superclasses or superinterfaces,
        // then we have to check that the method is either identical to or an override of a method that:
        // - is declared on a class which is a superclass of the declaring class, or
        // - is declared on an interface which implemented by the declaring class
        //
        // this is to satisfy the specification, which says: fallback method must be on the same class, a superclass
        // or an implemented interface of the class which declares the annotated method
        //
        // we fake this by checking that the matching method has the same name as one of the method declared on
        // the declaring class or any of its superclasses or any of its implemented interfaces (this is actually
        // quite precise, the only false positive would occur in presence of overloads)
        Set<String> declaredMethodNames = findDeclaredMethodNames(declaringClass);

        Deque<ClassWithTypeMapping> worklist = new ArrayDeque<>();
        {
            // add all superclasses first, so that they're preferred
            // interfaces are added during worklist iteration
            ClassInfo clazz = beanClass;
            TypeMapping typeMapping = new TypeMapping();
            worklist.add(new ClassWithTypeMapping(clazz, typeMapping));
            while (clazz.superName() != null) {
                ClassInfo superclass = index.getClassByName(clazz.superName());
                if (superclass == null) {
                    throw new IllegalArgumentException("Class not in index: " + clazz.superName());
                }
                Type genericSuperclass = clazz.superClassType();
                typeMapping = typeMapping.getDirectSupertypeMapping(superclass, genericSuperclass);
                worklist.add(new ClassWithTypeMapping(superclass, typeMapping));

                clazz = superclass;
            }
        }
        while (!worklist.isEmpty()) {
            ClassWithTypeMapping classWithTypeMapping = worklist.removeFirst();
            ClassInfo clazz = classWithTypeMapping.clazz;
            TypeMapping actualMapping = classWithTypeMapping.typeMapping;

            Set<MethodInfo> methods = getMethodsFromClass(clazz, name, expectedParameterTypes, expectedReturnType,
                    expectedExceptionParameter, declaringClass, actualMapping, expectedMapping);
            for (MethodInfo method : methods) {
                if (declaredMethodNames.contains(method.name())) {
                    result.add(method);
                    if (!expectedExceptionParameter) {
                        return result;
                    }
                }
            }

            List<DotName> interfaces = clazz.interfaceNames();
            for (int i = 0; i < interfaces.size(); i++) {
                ClassInfo iface = index.getClassByName(interfaces.get(i));
                if (iface == null) {
                    throw new IllegalArgumentException("Class not in index: " + interfaces.get(i));
                }
                Type genericIface = clazz.interfaceTypes().get(i);
                worklist.add(new ClassWithTypeMapping(iface,
                        actualMapping.getDirectSupertypeMapping(iface, genericIface)));
            }
        }

        return result;
    }

    private Set<String> findDeclaredMethodNames(ClassInfo declaringClass) {
        Set<String> result = new HashSet<>();

        Deque<ClassInfo> worklist = new ArrayDeque<>();
        worklist.add(declaringClass);
        while (!worklist.isEmpty()) {
            ClassInfo clazz = worklist.removeFirst();
            for (MethodInfo m : clazz.methods()) {
                result.add(m.name());
            }

            if (clazz.superName() != null) {
                ClassInfo superClass = index.getClassByName(clazz.superName());
                if (superClass != null) {
                    worklist.add(superClass);
                }
            }
            for (DotName interfaceName : clazz.interfaceNames()) {
                ClassInfo iface = index.getClassByName(interfaceName);
                if (iface != null) {
                    worklist.add(iface);
                }
            }
        }

        return result;
    }

    /**
     * Returns all methods that:
     * <ul>
     * <li>are declared directly on given {@code classToSearch},</li>
     * <li>have given {@code name},</li>
     * <li>have matching {@code parameterTypes},</li>
     * <li>have matching {@code returnType},</li>
     * <li>have an additional {@code exceptionParameter} if required,</li>
     * <li>are accessible from given {@code guardedMethodDeclaringClass}.</li>
     * </ul>
     */
    private Set<MethodInfo> getMethodsFromClass(ClassInfo classToSearch, String name, Type[] parameterTypes,
            Type returnType, boolean exceptionParameter, ClassInfo guardedMethodDeclaringClass,
            TypeMapping actualMapping, TypeMapping expectedMapping) {
        Set<MethodInfo> set = new HashSet<>();
        for (MethodInfo method : classToSearch.methods()) {
            if (method.name().equals(name)
                    && isAccessibleFrom(method, guardedMethodDeclaringClass)
                    && signaturesMatch(method, parameterTypes, returnType, exceptionParameter,
                            actualMapping, expectedMapping)) {
                set.add(method);
            }
        }
        return set;
    }

    private boolean isAccessibleFrom(MethodInfo method, ClassInfo guardedMethodDeclaringClass) {
        if (Modifier.isPublic(method.flags()) || Modifier.isProtected(method.flags())) {
            return true;
        }
        if (Modifier.isPrivate(method.flags())) {
            return method.declaringClass() == guardedMethodDeclaringClass;
        }
        // not public, not protected and not private => default
        // accessible only if in the same package
        return method.declaringClass().name().packagePrefixName()
                .equals(guardedMethodDeclaringClass.name().packagePrefixName());
    }

    private boolean signaturesMatch(MethodInfo method, Type[] expectedParameterTypes, Type expectedReturnType,
            boolean expectedExceptionParameter, TypeMapping actualMapping, TypeMapping expectedMapping) {
        int expectedParameters = expectedParameterTypes.length;
        if (expectedExceptionParameter) {
            // need to figure this out _before_ expanding the `expectedParameterTypes` array
            boolean kotlinSuspendingFunction = isKotlinSuspendingFunction(expectedParameterTypes);
            // adjust `expectedParameterTypes` so that there's one more element on the position
            // where the exception parameter should be, and the value on that position is `null`
            expectedParameterTypes = Arrays.copyOfRange(expectedParameterTypes, 0, expectedParameters + 1);
            if (kotlinSuspendingFunction) {
                expectedParameterTypes[expectedParameters] = expectedParameterTypes[expectedParameters - 1];
                expectedParameterTypes[expectedParameters - 1] = null;
            }
            expectedParameters++;
        }

        List<Type> methodParams = method.parameterTypes();
        if (expectedParameters != methodParams.size()) {
            return false;
        }

        for (int i = 0; i < expectedParameters; i++) {
            Type methodParam = methodParams.get(i);
            Type expectedParamType = expectedParameterTypes[i];
            if (expectedParamType != null) {
                if (!typeMatches(methodParam, expectedParamType, actualMapping, expectedMapping)) {
                    return false;
                }
            } else { // exception parameter
                boolean isThrowable = methodParam.kind() == Type.Kind.CLASS
                        && assignability.isAssignableFrom(ClassType.create(Throwable.class), methodParam);
                if (!isThrowable) {
                    return false;
                }
            }
        }

        if (!typeMatches(method.returnType(), expectedReturnType, actualMapping, expectedMapping)) {
            return false;
        }

        return true;
    }

    private static boolean isKotlinSuspendingFunction(Type[] parameterTypes) {
        int params = parameterTypes.length;
        if (params > 0) {
            return parameterTypes[params - 1].name().equals(KotlinDotNames.CONTINUATION);
        }
        return false;
    }

    private boolean typeMatches(Type actualType, Type expectedType,
            TypeMapping actualMapping, TypeMapping expectedMapping) {
        actualType = actualMapping.map(actualType);
        expectedType = expectedMapping.map(expectedType);

        if (actualType.kind() == Type.Kind.CLASS
                || actualType.kind() == Type.Kind.PRIMITIVE
                || actualType.kind() == Type.Kind.VOID) {
            return expectedType.equals(actualType);
        } else if (actualType.kind() == Type.Kind.ARRAY && expectedType.kind() == Type.Kind.ARRAY) {
            return typeMatches(actualType.asArrayType().componentType(), expectedType.asArrayType().componentType(),
                    actualMapping, expectedMapping);
        } else if (actualType.kind() == Type.Kind.PARAMETERIZED_TYPE && expectedType.kind() == Type.Kind.PARAMETERIZED_TYPE) {
            return parameterizedTypeMatches(actualType.asParameterizedType(), expectedType.asParameterizedType(),
                    actualMapping, expectedMapping);
        } else if (actualType.kind() == Type.Kind.WILDCARD_TYPE && expectedType.kind() == Type.Kind.WILDCARD_TYPE) {
            return wildcardTypeMatches(actualType.asWildcardType(), expectedType.asWildcardType(),
                    actualMapping, expectedMapping);
        } else {
            return false;
        }
    }

    private boolean wildcardTypeMatches(WildcardType actualType, WildcardType expectedType,
            TypeMapping actualMapping, TypeMapping expectedMapping) {
        Type actualLowerBound = actualType.superBound();
        Type expectedLowerBound = expectedType.superBound();
        boolean lowerBoundsMatch = (actualLowerBound == null && expectedLowerBound == null)
                || (actualLowerBound != null && expectedLowerBound != null
                        && typeMatches(actualLowerBound, expectedLowerBound, actualMapping, expectedMapping));
        boolean upperBoundsMatch = typeMatches(actualType.extendsBound(), expectedType.extendsBound(),
                actualMapping, expectedMapping);
        return lowerBoundsMatch && upperBoundsMatch;
    }

    private boolean parameterizedTypeMatches(ParameterizedType actualType, ParameterizedType expectedType,
            TypeMapping actualMapping, TypeMapping expectedMapping) {
        boolean genericClassMatch = typeMatches(ClassType.create(actualType.name()), ClassType.create(expectedType.name()),
                actualMapping, expectedMapping);
        boolean typeArgumentsMatch = typeListMatches(actualType.arguments(), expectedType.arguments(),
                actualMapping, expectedMapping);
        return genericClassMatch && typeArgumentsMatch;
    }

    private boolean typeListMatches(List<Type> actualTypes, List<Type> expectedTypes,
            TypeMapping actualMapping, TypeMapping expectedMapping) {
        if (actualTypes.size() != expectedTypes.size()) {
            return false;
        }
        for (int i = 0; i < actualTypes.size(); i++) {
            if (!typeMatches(actualTypes.get(i), expectedTypes.get(i), actualMapping, expectedMapping)) {
                return false;
            }
        }
        return true;
    }

    private record ClassWithTypeMapping(ClassInfo clazz, TypeMapping typeMapping) {
    }

    private record TypeMapping(Map<Type, Type> map) {
        private TypeMapping() {
            this(Collections.emptyMap());
        }

        /**
         * Bean class can be a subclass of the class that declares the guarded method.
         * This method returns a mapping of the type parameters of the method's declaring class
         * to the type arguments provided on the bean class or any class between it and the declaring class.
         *
         * @param beanClass class of the bean which has the guarded method
         * @param declaringClass class that actually declares the guarded method
         * @param index index to use for locating superclasses
         * @return type mapping
         */
        private static TypeMapping createFor(ClassInfo beanClass, ClassInfo declaringClass, IndexView index) {
            TypeMapping result = new TypeMapping();
            if (beanClass == declaringClass) {
                return result;
            }

            ClassInfo current = beanClass;
            while (current != declaringClass && current != null) {
                if (current.superName() == null) {
                    break;
                }
                ClassInfo superClass = index.getClassByName(current.superName());
                if (superClass == null) {
                    throw new IllegalArgumentException("Class not in index: " + current.superName());
                }
                result = result.getDirectSupertypeMapping(superClass, current.superClassType());
                current = superClass;
            }

            return result;
        }

        private Type map(Type type) {
            Type result = map.get(type);
            return result != null ? result : type;
        }

        private TypeMapping getDirectSupertypeMapping(ClassInfo supertype, Type genericSupertype) {
            List<TypeVariable> typeParameters = supertype.typeParameters();
            List<Type> typeArguments = genericSupertype.kind() == Type.Kind.PARAMETERIZED_TYPE
                    ? genericSupertype.asParameterizedType().arguments()
                    : Collections.emptyList();

            Map<Type, Type> result = new HashMap<>();

            for (int i = 0; i < typeArguments.size(); i++) {
                Type typeArgument = typeArguments.get(i);
                if (typeArgument.kind() == Type.Kind.CLASS) {
                    result.put(typeParameters.get(i), typeArgument);
                } else {
                    Type type = map.get(typeArgument);
                    result.put(typeParameters.get(i), type != null ? type : typeArgument);
                }
            }

            return new TypeMapping(result);
        }
    }
}
