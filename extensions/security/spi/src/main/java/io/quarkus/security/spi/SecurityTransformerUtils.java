package io.quarkus.security.spi;

import java.util.Collection;
import java.util.Optional;
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
public final class SecurityTransformerUtils {
    public static final DotName DENY_ALL = DotName.createSimple(DenyAll.class.getName());
    private static final Set<DotName> SECURITY_ANNOTATIONS = Set.of(DotName.createSimple(RolesAllowed.class.getName()),
            DotName.createSimple(PermissionsAllowed.class.getName()),
            DotName.createSimple(PermissionsAllowed.List.class.getName()),
            DotName.createSimple(Authenticated.class.getName()),
            DotName.createSimple(DenyAll.class.getName()),
            DotName.createSimple(PermitAll.class.getName()));

    private SecurityTransformerUtils() {
        // utils
    }

    public static boolean hasSecurityAnnotation(MethodInfo methodInfo) {
        return findFirstStandardSecurityAnnotation(methodInfo).isPresent();
    }

    public static boolean hasSecurityAnnotation(ClassInfo classInfo) {
        return findFirstStandardSecurityAnnotation(classInfo).isPresent();
    }

    public static boolean hasSecurityAnnotation(Collection<AnnotationInstance> instances) {
        return findFirstStandardSecurityAnnotation(instances).isPresent();
    }

    public static boolean isStandardSecurityAnnotation(AnnotationInstance annotationInstance) {
        return SECURITY_ANNOTATIONS.contains(annotationInstance.name());
    }

    public static Optional<AnnotationInstance> findFirstStandardSecurityAnnotation(MethodInfo methodInfo) {
        return findFirstStandardSecurityAnnotation(methodInfo.annotations());
    }

    public static Optional<AnnotationInstance> findFirstStandardSecurityAnnotation(ClassInfo classInfo) {
        return findFirstStandardSecurityAnnotation(classInfo.declaredAnnotations());
    }

    public static Optional<AnnotationInstance> findFirstStandardSecurityAnnotation(Collection<AnnotationInstance> instances) {
        for (AnnotationInstance instance : instances) {
            if (SECURITY_ANNOTATIONS.contains(instance.name())) {
                return Optional.of(instance);
            }
        }
        return Optional.empty();
    }

}
