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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.util.Nonbinding;

public final class Qualifiers {

    public static final Set<Annotation> DEFAULT_QUALIFIERS = initDefaultQualifiers();

    private Qualifiers() {
    }

    static boolean hasQualifiers(InjectableBean<?> bean, Annotation... requiredQualifiers) {
        for (Annotation qualifier : requiredQualifiers) {
            if (!hasQualifier(bean, qualifier)) {
                return false;
            }
        }
        return true;
    }

    static boolean hasQualifier(InjectableBean<?> bean, Annotation requiredQualifier) {

        Class<? extends Annotation> requiredQualifierClass = requiredQualifier.annotationType();
        Method[] members = requiredQualifierClass.getDeclaredMethods();

        for (Annotation qualifier : bean.getQualifiers()) {
            Class<? extends Annotation> qualifierClass = qualifier.annotationType();
            if (qualifierClass.equals(requiredQualifier.annotationType())) {
                boolean matches = true;
                for (Method value : members) {
                    if (!value.isAnnotationPresent(Nonbinding.class)
                            && !invoke(value, requiredQualifier).equals(invoke(value, qualifier))) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean hasQualifier(Set<Annotation> qualifiers, Annotation requiredQualifier) {

        Class<? extends Annotation> requiredQualifierClass = requiredQualifier.annotationType();
        Method[] members = requiredQualifierClass.getDeclaredMethods();

        for (Annotation qualifier : qualifiers) {
            Class<? extends Annotation> qualifierClass = qualifier.annotationType();
            if (qualifierClass.equals(requiredQualifier.annotationType())) {
                boolean matches = true;
                for (Method value : members) {
                    if (!value.isAnnotationPresent(Nonbinding.class)
                            && !invoke(value, requiredQualifier).equals(invoke(value, qualifier))) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean isSubset(Set<Annotation> observedQualifiers, Set<Annotation> eventQualifiers) {
        for (Annotation required : observedQualifiers) {
            if (!hasQualifier(eventQualifiers, required)) {
                return false;
            }
        }
        return true;
    }

    private static Set<Annotation> initDefaultQualifiers() {
        Set<Annotation> qualifiers = new HashSet<>();
        qualifiers.add(Default.Literal.INSTANCE);
        qualifiers.add(Any.Literal.INSTANCE);
        return qualifiers;
    }

    private static Object invoke(Method method, Object instance) {
        try {
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            return method.invoke(instance);
        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(
                    "Error checking value of member method " + method.getName() + " on " + method.getDeclaringClass(), e);
        }
    }

}
