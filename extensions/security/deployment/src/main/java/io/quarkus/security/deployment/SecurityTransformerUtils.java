package io.quarkus.security.deployment;

import java.util.Set;

import javax.annotation.security.DenyAll;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class SecurityTransformerUtils {
    public static final DotName DENY_ALL = DotName.createSimple(DenyAll.class.getName());
    private static final Set<DotName> SECURITY_ANNOTATIONS = SecurityAnnotationsRegistrar.SECURITY_BINDINGS.keySet();

    public static boolean hasSecurityAnnotation(MethodInfo methodInfo) {
        for (AnnotationInstance annotation : methodInfo.annotations()) {
            if (SECURITY_ANNOTATIONS.contains(annotation.name())) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasSecurityAnnotation(ClassInfo classInfo) {
        for (AnnotationInstance classAnnotation : classInfo.classAnnotations()) {
            if (SECURITY_ANNOTATIONS.contains(classAnnotation.name())) {
                return true;
            }
        }

        return false;
    }
}
