package io.quarkus.deployment.steps;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

final class KotlinUtil {

    private static final DotName KOTLIN_METADATA_ANNOTATION = DotName.createSimple("kotlin.Metadata");

    private KotlinUtil() {
    }

    static boolean isKotlinClass(ClassInfo classInfo) {
        return classInfo.hasDeclaredAnnotation(KOTLIN_METADATA_ANNOTATION);
    }
}
