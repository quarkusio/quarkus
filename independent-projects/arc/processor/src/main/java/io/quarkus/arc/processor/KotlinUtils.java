package io.quarkus.arc.processor;

import java.lang.reflect.Modifier;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

public class KotlinUtils {
    public static boolean isKotlinClass(ClassInfo clazz) {
        return clazz.hasDeclaredAnnotation(KotlinDotNames.METADATA);
    }

    public static boolean isKotlinMethod(MethodInfo method) {
        return isKotlinClass(method.declaringClass());
    }
}
