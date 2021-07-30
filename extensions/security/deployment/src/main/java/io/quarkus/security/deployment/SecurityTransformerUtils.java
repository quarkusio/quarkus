package io.quarkus.security.deployment;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import javax.annotation.security.DenyAll;
import javax.annotation.security.RolesAllowed;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class SecurityTransformerUtils {
    public static final DotName DENY_ALL = DotName.createSimple(DenyAll.class.getName());
    public static final DotName ROLES_ALLOWED = DotName.createSimple(RolesAllowed.class.getName());
    private static final Set<DotName> SECURITY_ANNOTATIONS = SecurityAnnotationsRegistrar.SECURITY_BINDINGS.keySet();

    public static boolean hasStandardSecurityAnnotation(MethodInfo methodInfo) {
        return hasStandardSecurityAnnotation(methodInfo.annotations());
    }

    public static boolean hasStandardSecurityAnnotation(ClassInfo classInfo) {
        return hasStandardSecurityAnnotation(classInfo.classAnnotations());
    }

    private static boolean hasStandardSecurityAnnotation(Collection<AnnotationInstance> instances) {
        for (AnnotationInstance instance : instances) {
            if (SECURITY_ANNOTATIONS.contains(instance.name())) {
                return true;
            }
        }
        return false;
    }

    public static Optional<AnnotationInstance> findFirstStandardSecurityAnnotation(MethodInfo methodInfo) {
        return findFirstStandardSecurityAnnotation(methodInfo.annotations());
    }

    public static Optional<AnnotationInstance> findFirstStandardSecurityAnnotation(ClassInfo classInfo) {
        return findFirstStandardSecurityAnnotation(classInfo.classAnnotations());
    }

    private static Optional<AnnotationInstance> findFirstStandardSecurityAnnotation(Collection<AnnotationInstance> instances) {
        for (AnnotationInstance instance : instances) {
            if (SECURITY_ANNOTATIONS.contains(instance.name())) {
                return Optional.of(instance);
            }
        }
        return Optional.empty();
    }

}
