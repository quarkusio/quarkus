package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;

public final class Qualifiers {

    public static final Set<Annotation> DEFAULT_QUALIFIERS = Set.of(Default.Literal.INSTANCE, Any.Literal.INSTANCE);

    public static final Set<Annotation> IP_DEFAULT_QUALIFIERS = Set.of(Default.Literal.INSTANCE);

    final Set<String> allQualifiers;
    // qualifier class name -> non-binding members (can be empty but never null)
    final Map<String, Set<String>> qualifierNonbindingMembers;

    Qualifiers(Set<String> qualifiers, Map<String, Set<String>> qualifierNonbindingMembers) {
        this.allQualifiers = qualifiers;
        this.qualifierNonbindingMembers = qualifierNonbindingMembers;
    }

    boolean isRegistered(Class<? extends Annotation> annotationType) {
        return allQualifiers.contains(annotationType.getName());
    }

    void verify(Collection<Annotation> qualifiers) {
        if (qualifiers.isEmpty()) {
            return;
        }
        if (qualifiers.size() == 1) {
            verifyQualifier(qualifiers.iterator().next().annotationType());
        } else {
            Map<Class<? extends Annotation>, Integer> timesQualifierWasSeen = new HashMap<>();
            for (Annotation qualifier : qualifiers) {
                verifyQualifier(qualifier.annotationType());
                timesQualifierWasSeen.compute(qualifier.annotationType(), TimesSeenBiFunction.INSTANCE);
            }
            checkQualifiersForDuplicates(timesQualifierWasSeen);
        }
    }

    void verify(Annotation[] qualifiers) {
        if (qualifiers.length == 0) {
            return;
        }
        if (qualifiers.length == 1) {
            verifyQualifier(qualifiers[0].annotationType());
        } else {
            Map<Class<? extends Annotation>, Integer> timesQualifierWasSeen = new HashMap<>();
            for (Annotation qualifier : qualifiers) {
                verifyQualifier(qualifier.annotationType());
                timesQualifierWasSeen.compute(qualifier.annotationType(), TimesSeenBiFunction.INSTANCE);
            }
            checkQualifiersForDuplicates(timesQualifierWasSeen);
        }
    }

    // in various cases, specification requires to check qualifiers for duplicates and throw IAE
    private static void checkQualifiersForDuplicates(Map<Class<? extends Annotation>, Integer> timesQualifierSeen) {
        for (Entry<Class<? extends Annotation>, Integer> entry : timesQualifierSeen.entrySet()) {
            checkQualifiersForDuplicates(entry.getKey(), entry.getValue());
        }
    }

    private static void checkQualifiersForDuplicates(Class<? extends Annotation> aClass, Integer times) {
        if (times > 1 && (aClass.getAnnotation(Repeatable.class) == null)) {
            throw new IllegalArgumentException("The qualifier " + aClass + " was used repeatedly " +
                    "but it is not annotated with @java.lang.annotation.Repeatable");
        }
    }

    boolean hasQualifiers(Set<Annotation> beanQualifiers, Annotation... requiredQualifiers) {
        return Annotations.areAllPresent(Set.of(requiredQualifiers), beanQualifiers, qualifierNonbindingMembers);
    }

    boolean isSubset(Set<Annotation> observedQualifiers, Set<Annotation> eventQualifiers) {
        return Annotations.areAllPresent(observedQualifiers, eventQualifiers, qualifierNonbindingMembers);
    }

    private void verifyQualifier(Class<? extends Annotation> annotationType) {
        if (!allQualifiers.contains(annotationType.getName())) {
            throw new IllegalArgumentException("Annotation is not a registered qualifier: " + annotationType);
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
