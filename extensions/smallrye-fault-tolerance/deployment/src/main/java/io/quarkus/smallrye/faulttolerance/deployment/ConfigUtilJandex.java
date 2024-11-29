package io.quarkus.smallrye.faulttolerance.deployment;

import java.lang.annotation.Annotation;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;

import io.smallrye.faulttolerance.config.ConfigPrefix;
import io.smallrye.faulttolerance.config.NewConfig;

// copy of `io.smallrye.faulttolerance.config.ConfigUtil` and translation from reflection to Jandex
final class ConfigUtilJandex {
    static final String GLOBAL = "global";

    static String newKey(Class<? extends Annotation> annotation, String member, MethodInfo declaringMethod) {
        return ConfigPrefix.VALUE + "\"" + declaringMethod.declaringClass().name() + "/" + declaringMethod.name()
                + "\"." + NewConfig.get(annotation, member);
    }

    static String oldKey(Class<? extends Annotation> annotation, String member, MethodInfo declaringMethod) {
        return declaringMethod.declaringClass().name() + "/" + declaringMethod.name()
                + "/" + annotation.getSimpleName() + "/" + member;
    }

    static String newKey(Class<? extends Annotation> annotation, String member, ClassInfo declaringClass) {
        return ConfigPrefix.VALUE + "\"" + declaringClass.name() + "\"."
                + NewConfig.get(annotation, member);
    }

    static String oldKey(Class<? extends Annotation> annotation, String member, ClassInfo declaringClass) {
        return declaringClass.name() + "/" + annotation.getSimpleName() + "/" + member;
    }

    static String newKey(Class<? extends Annotation> annotation, String member) {
        return ConfigPrefix.VALUE + GLOBAL + "." + NewConfig.get(annotation, member);
    }

    static String oldKey(Class<? extends Annotation> annotation, String member) {
        return annotation.getSimpleName() + "/" + member;
    }

    // no need to have the `isEnabled()` method, that is only needed at runtime
}
