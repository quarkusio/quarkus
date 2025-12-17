package io.quarkus.security.spi;

import java.util.Objects;
import java.util.function.Predicate;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Security annotations on interfaces are in most cases not inherited by interface implementors.
 * This build item allows to register interfaces whose implementors will inherit security annotations.
 * If this build item is used extensively, it can get very complex, and we would create situations,
 * where users can hardly tell precedence, like what will happen if different repeatable annotation instance
 * (like the {@code PermissionsAllowed} annotation) is placed on both implemented and interface method.
 * Or does method-level interface annotation take precedence over implementors class-level annotation.
 * Examples could continue, for which reason we aim to support simple cases for now.
 * <p>
 * The all the implementors of interfaces matched by this build item annotation will inherit
 * security annotations from interface methods and the interface class-level security annotations only
 * apply directly on the methods declared on the interface. All scenarios that are supposed to work are tested.
 * Any scenario that is not working is not yet supported.
 */
public final class SecuredInterfaceAnnotationBuildItem extends MultiBuildItem {

    private enum SecuredAnnotationTargetKind {
        METHOD,
        CLASS
    }

    private final DotName annotationName;

    private final SecuredAnnotationTargetKind targetKind;

    private SecuredInterfaceAnnotationBuildItem(DotName annotationName, SecuredAnnotationTargetKind targetKind) {
        this.annotationName = Objects.requireNonNull(annotationName);
        this.targetKind = targetKind;
    }

    public Predicate<ClassInfo> getIsInterfaceWithTransformations() {
        return switch (targetKind) {
            case CLASS -> ci -> ci.isInterface() && ci.hasDeclaredAnnotation(annotationName);
            case METHOD -> ci -> ci.isInterface() && ci.hasAnnotation(annotationName);
        };
    }

    public DotName getAnnotationName() {
        return annotationName;
    }

    public static SecuredInterfaceAnnotationBuildItem ofClassAnnotation(String annotationName) {
        return new SecuredInterfaceAnnotationBuildItem(DotName.createSimple(annotationName), SecuredAnnotationTargetKind.CLASS);
    }

    public static SecuredInterfaceAnnotationBuildItem ofMethodAnnotation(Class<?> annotation) {
        return new SecuredInterfaceAnnotationBuildItem(DotName.createSimple(annotation), SecuredAnnotationTargetKind.METHOD);
    }
}
