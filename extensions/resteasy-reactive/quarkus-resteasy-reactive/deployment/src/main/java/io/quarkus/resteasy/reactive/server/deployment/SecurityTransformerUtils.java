package io.quarkus.resteasy.reactive.server.deployment;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import io.quarkus.security.Authenticated;

public class SecurityTransformerUtils {
    public static final Set<DotName> SECURITY_BINDINGS = new HashSet<>();

    static {
        // keep the contents the same as in io.quarkus.resteasy.deployment.SecurityTransformerUtils
        SECURITY_BINDINGS.add(DotName.createSimple(RolesAllowed.class.getName()));
        SECURITY_BINDINGS.add(DotName.createSimple(Authenticated.class.getName()));
        SECURITY_BINDINGS.add(DotName.createSimple(DenyAll.class.getName()));
        SECURITY_BINDINGS.add(DotName.createSimple(PermitAll.class.getName()));
    }

    public static boolean hasStandardSecurityAnnotation(MethodInfo methodInfo) {
        return hasStandardSecurityAnnotation(methodInfo.annotations());
    }

    public static boolean hasStandardSecurityAnnotation(ClassInfo classInfo) {
        return hasStandardSecurityAnnotation(classInfo.classAnnotations());
    }

    private static boolean hasStandardSecurityAnnotation(Collection<AnnotationInstance> instances) {
        for (AnnotationInstance instance : instances) {
            if (SECURITY_BINDINGS.contains(instance.name())) {
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
            if (SECURITY_BINDINGS.contains(instance.name())) {
                return Optional.of(instance);
            }
        }
        return Optional.empty();
    }

}
