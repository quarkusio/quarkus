package io.quarkus.security.spi;

import java.util.Set;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import io.quarkus.security.Authenticated;
import io.quarkus.security.PermissionsAllowed;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class SecurityTransformerUtils {
    public static final DotName DENY_ALL = DotName.createSimple(DenyAll.class.getName());
    private static final Set<DotName> SECURITY_ANNOTATIONS = Set.of(DotName.createSimple(RolesAllowed.class.getName()),
            DotName.createSimple(PermissionsAllowed.class.getName()),
            DotName.createSimple(Authenticated.class.getName()),
            DotName.createSimple(DenyAll.class.getName()),
            DotName.createSimple(PermitAll.class.getName()));

    public static boolean hasSecurityAnnotation(MethodInfo methodInfo) {
        for (AnnotationInstance annotation : methodInfo.annotations()) {
            if (SECURITY_ANNOTATIONS.contains(annotation.name())) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasSecurityAnnotation(ClassInfo classInfo) {
        for (AnnotationInstance classAnnotation : classInfo.declaredAnnotations()) {
            if (SECURITY_ANNOTATIONS.contains(classAnnotation.name())) {
                return true;
            }
        }

        return false;
    }
}
