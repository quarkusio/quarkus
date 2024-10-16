package io.quarkus.security.spi;

import java.util.List;
import java.util.Optional;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Contains transitive {@link io.quarkus.security.PermissionsAllowed} instances.
 * The {@link io.quarkus.security.PermissionsAllowed} annotation supports meta-annotation
 * defined by users. Methods and classes annotated with these meta-annotations are collected
 * and new {@link AnnotationInstance}s are created for them.
 * Newly created instances are carried in the {@link #transitiveInstances} field.
 */
public final class PermissionsAllowedMetaAnnotationBuildItem extends SimpleBuildItem {

    private final List<DotName> metaAnnotationNames;
    private final boolean empty;
    private final List<AnnotationInstance> transitiveInstances;

    public PermissionsAllowedMetaAnnotationBuildItem(List<AnnotationInstance> transitiveInstances,
            List<DotName> metaAnnotationNames) {
        this.transitiveInstances = List.copyOf(transitiveInstances);
        this.metaAnnotationNames = List.copyOf(metaAnnotationNames);
        this.empty = transitiveInstances.isEmpty();
    }

    public boolean hasPermissionsAllowed(MethodInfo methodInfo) {
        if (empty) {
            return false;
        }
        return hasPermissionsAllowed(methodInfo.annotations());
    }

    public boolean hasPermissionsAllowed(ClassInfo classInfo) {
        if (empty) {
            return false;
        }
        return hasPermissionsAllowed(classInfo.declaredAnnotations());
    }

    public List<AnnotationInstance> getTransitiveInstances() {
        return transitiveInstances;
    }

    private boolean hasPermissionsAllowed(List<AnnotationInstance> instances) {
        return instances.stream().anyMatch(ai -> metaAnnotationNames.contains(ai.name()));
    }

    public Optional<AnnotationInstance> findPermissionsAllowedInstance(ClassInfo classInfo) {
        if (empty) {
            return Optional.empty();
        }
        return transitiveInstances
                .stream()
                .filter(ai -> ai.target().kind() == AnnotationTarget.Kind.CLASS)
                .filter(ai -> ai.target().asClass().name().equals(classInfo.name()))
                // not repeatable on class-level, therefore we can just find the first one
                .findFirst();
    }
}
