package io.quarkus.smallrye.faulttolerance.deployment;

import java.lang.annotation.Annotation;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;

import io.smallrye.config.common.utils.StringUtil;
import io.smallrye.faulttolerance.autoconfig.ConfigConstants;

// copy of a past version of `io.smallrye.faulttolerance.basicconfig.ConfigUtil` and translation from reflection to Jandex
final class ConfigUtilJandex {
    static String newKey(Class<? extends Annotation> annotation, String member, MethodInfo declaringMethod) {
        return ConfigConstants.PREFIX + "\"" + declaringMethod.declaringClass().name() + "/" + declaringMethod.name()
                + "\"." + newAnnotationName(annotation) + "." + newMemberName(member);
    }

    static String oldKey(Class<? extends Annotation> annotation, String member, MethodInfo declaringMethod) {
        return declaringMethod.declaringClass().name() + "/" + declaringMethod.name()
                + "/" + annotation.getSimpleName() + "/" + member;
    }

    static String newKey(Class<? extends Annotation> annotation, String member, ClassInfo declaringClass) {
        return ConfigConstants.PREFIX + "\"" + declaringClass.name() + "\"."
                + newAnnotationName(annotation) + "." + newMemberName(member);
    }

    static String oldKey(Class<? extends Annotation> annotation, String member, ClassInfo declaringClass) {
        return declaringClass.name() + "/" + annotation.getSimpleName() + "/" + member;
    }

    static String newKey(Class<? extends Annotation> annotation, String member) {
        return ConfigConstants.PREFIX + ConfigConstants.GLOBAL + "."
                + newAnnotationName(annotation) + "." + newMemberName(member);
    }

    static String oldKey(Class<? extends Annotation> annotation, String member) {
        return annotation.getSimpleName() + "/" + member;
    }

    // no need to have the `isEnabled()` method, that is only needed at runtime

    // ---
    // these 2 methods are mostly copied from `io.smallrye.faulttolerance.autoconfig.processor.AutoConfigProcessor`

    private static String newAnnotationName(Class<? extends Annotation> annotationDeclaration) {
        return StringUtil.skewer(annotationDeclaration.getSimpleName());
    }

    private static String newMemberName(String annotationMember) {
        String name = switch (annotationMember) {
            case "jitterDelayUnit" -> "jitterUnit";
            case "durationUnit" -> "maxDurationUnit";
            default -> annotationMember;
        };
        return StringUtil.skewer(name);
    }
}
