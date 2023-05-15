package io.quarkus.arc.impl;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public final class InterceptorBindings {
    private final Set<String> allInterceptorBindings;
    private final Map<Class<? extends Annotation>, Set<Annotation>> transitiveInterceptorBindings;

    InterceptorBindings(Set<String> interceptorBindings,
            Map<Class<? extends Annotation>, Set<Annotation>> transitiveInterceptorBindings) {
        this.allInterceptorBindings = interceptorBindings;
        this.transitiveInterceptorBindings = transitiveInterceptorBindings;
    }

    boolean isRegistered(Class<? extends Annotation> annotationType) {
        return allInterceptorBindings.contains(annotationType.getName());
    }

    Set<Annotation> getTransitive(Class<? extends Annotation> interceptorBinding) {
        return transitiveInterceptorBindings.get(interceptorBinding);
    }

    void verify(Annotation[] interceptorBindings) {
        if (interceptorBindings.length == 0) {
            return;
        }
        if (interceptorBindings.length == 1) {
            verifyInterceptorBinding(interceptorBindings[0].annotationType());
        } else {
            Map<Class<? extends Annotation>, Integer> timesQualifierWasSeen = new HashMap<>();
            for (Annotation interceptorBinding : interceptorBindings) {
                verifyInterceptorBinding(interceptorBinding.annotationType());
                timesQualifierWasSeen.compute(interceptorBinding.annotationType(), TimesSeenBiFunction.INSTANCE);
            }
            checkInterceptorBindingsForDuplicates(timesQualifierWasSeen);
        }
    }

    // in various cases, specification requires to check interceptor bindings for duplicates and throw IAE
    private static void checkInterceptorBindingsForDuplicates(Map<Class<? extends Annotation>, Integer> timesSeen) {
        timesSeen.forEach(InterceptorBindings::checkInterceptorBindingsForDuplicates);
    }

    private static void checkInterceptorBindingsForDuplicates(Class<? extends Annotation> annClass, Integer timesSeen) {
        if (timesSeen > 1 && !annClass.isAnnotationPresent(Repeatable.class)) {
            throw new IllegalArgumentException("Interceptor binding " + annClass
                    + " was used repeatedly but is not @Repeatable");
        }
    }

    private void verifyInterceptorBinding(Class<? extends Annotation> annotationType) {
        if (!allInterceptorBindings.contains(annotationType.getName())) {
            throw new IllegalArgumentException("Annotation is not a registered interceptor binding: " + annotationType);
        }
    }

    private static class TimesSeenBiFunction implements BiFunction<Class<? extends Annotation>, Integer, Integer> {
        private static final TimesSeenBiFunction INSTANCE = new TimesSeenBiFunction();

        @Override
        public Integer apply(Class<? extends Annotation> k, Integer v) {
            return v == null ? 1 : v + 1;
        }
    }
}
