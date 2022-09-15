package io.quarkus.arc.impl;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public final class Qualifiers {

    public static final Set<Annotation> DEFAULT_QUALIFIERS = Set.of(Default.Literal.INSTANCE, Any.Literal.INSTANCE);

    @SuppressWarnings("unused")
    public static final Set<Annotation> IP_DEFAULT_QUALIFIERS = Collections.singleton(Default.Literal.INSTANCE);

    private Qualifiers() {
    }

    static void verify(Collection<Annotation> qualifiers, Set<String> customQualifiers) {
        if (qualifiers.isEmpty()) {
            return;
        }

        if (qualifiers.size() == 1) {
            verifyQualifier(qualifiers.iterator().next().annotationType(), customQualifiers);
        } else {
            Map<Class<? extends Annotation>, Integer> timesQualifierWasSeen = new HashMap<>();
            for (Annotation qualifier : qualifiers) {
                verifyQualifier(qualifier.annotationType(), customQualifiers);
                timesQualifierWasSeen.compute(qualifier.annotationType(), TimesSeenBiFunction.INSTANCE);
            }
            checkQualifiersForDuplicates(timesQualifierWasSeen);
        }
    }

    static void verify(Annotation[] qualifiers, Set<String> customQualifiers) {
        if (qualifiers.length == 0) {
            return;
        }

        if (qualifiers.length == 1) {
            verifyQualifier(qualifiers[0].annotationType(), customQualifiers);
        } else {
            Map<Class<? extends Annotation>, Integer> timesQualifierWasSeen = new HashMap<>();
            for (Annotation qualifier : qualifiers) {
                verifyQualifier(qualifier.annotationType(), customQualifiers);
                timesQualifierWasSeen.compute(qualifier.annotationType(), TimesSeenBiFunction.INSTANCE);
            }
            checkQualifiersForDuplicates(timesQualifierWasSeen);
        }
    }

    // in various cases, specification requires to check qualifiers for duplicates and throw IAE
    private static void checkQualifiersForDuplicates(Map<Class<? extends Annotation>, Integer> timesQualifierSeen) {
        timesQualifierSeen.forEach(Qualifiers::checkQualifiersForDuplicates);
    }

    private static void checkQualifiersForDuplicates(Class<? extends Annotation> aClass, Integer times) {
        if (times > 1 && (aClass.getAnnotation(Repeatable.class) == null)) {
            throw new IllegalArgumentException("The qualifier " + aClass + " was used repeatedly " +
                    "but it is not annotated with @java.lang.annotation.Repeatable");
        }
    }

    static boolean hasQualifiers(Set<Annotation> beanQualifiers, Map<String, Set<String>> qualifierNonbindingMembers,
            Annotation... requiredQualifiers) {
        for (Annotation qualifier : requiredQualifiers) {
            if (!hasQualifier(beanQualifiers, qualifier, qualifierNonbindingMembers)) {
                return false;
            }
        }
        return true;
    }

    static boolean hasQualifier(Iterable<Annotation> qualifiers, Annotation requiredQualifier,
            Map<String, Set<String>> qualifierNonbindingMembers) {

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
                if (!qualifierNonbindingMembers.isEmpty()) {
                    Set<String> nonbindingMembers = qualifierNonbindingMembers.get(qualifierClass.getName());
                    if (nonbindingMembers != null && nonbindingMembers.contains(value.getName())) {
                        continue;
                    }
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

    static boolean isSubset(Set<Annotation> observedQualifiers, Set<Annotation> eventQualifiers,
            Map<String, Set<String>> qualifierNonbindingMembers) {
        for (Annotation required : observedQualifiers) {
            if (!hasQualifier(eventQualifiers, required, qualifierNonbindingMembers)) {
                return false;
            }
        }
        return true;
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

    private static void verifyQualifier(Class<? extends Annotation> annotationType, Set<String> customQualifiers) {
        if (customQualifiers.contains(annotationType.getName())) {
            return;
        }

        if (!annotationType.isAnnotationPresent(Qualifier.class)) {
            throw new IllegalArgumentException("Annotation is not a qualifier: " + annotationType);
        }
    }

    private static class TimesSeenBiFunction implements BiFunction<Class<? extends Annotation>, Integer, Integer> {

        private static final TimesSeenBiFunction INSTANCE = new TimesSeenBiFunction();

        private TimesSeenBiFunction() {
        }

        @Override
        public Integer apply(Class<? extends Annotation> k, Integer v) {
            return (v == null) ? 1 : (v + 1);
        }
    }
}
