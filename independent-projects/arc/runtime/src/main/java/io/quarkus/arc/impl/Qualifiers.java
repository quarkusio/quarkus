package io.quarkus.arc.impl;

import io.quarkus.arc.InjectableBean;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.util.Nonbinding;

public final class Qualifiers {

    public static final Set<Annotation> DEFAULT_QUALIFIERS = initDefaultQualifiers();

    public static final Set<Annotation> IP_DEFAULT_QUALIFIERS = Collections.singleton(Default.Literal.INSTANCE);

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
        return hasQualifier(bean.getQualifiers(), requiredQualifier);
    }

    static boolean hasQualifier(Iterable<Annotation> qualifiers, Annotation requiredQualifier) {

        Class<? extends Annotation> requiredQualifierClass = requiredQualifier.annotationType();
        Method[] members = requiredQualifierClass.getDeclaredMethods();

        for (Annotation qualifier : qualifiers) {
            Class<? extends Annotation> qualifierClass = qualifier.annotationType();
            if (!qualifierClass.equals(requiredQualifierClass)) {
                continue;
            }
            boolean matches = true;
            for (Method value : members) {
                if (value.isAnnotationPresent(Nonbinding.class)) {
                    continue;
                }
                Object val1 = invoke(value, requiredQualifier);
                Object val2 = invoke(value, qualifier);
                if (val1.getClass().isArray()) {
                    if (!val2.getClass().isArray() || !Arrays.equals((Object[]) val1, (Object[]) val2)) {
                        matches = false;
                        break;
                    }
                } else if (!val1.equals(val2)) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return true;
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
