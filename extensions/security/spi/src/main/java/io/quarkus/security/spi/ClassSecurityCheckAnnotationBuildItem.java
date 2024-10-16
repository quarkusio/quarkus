package io.quarkus.security.spi;

import java.util.Objects;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Allows to create additional security checks for standard security annotations defined on a class level.
 * We strongly recommended to secure CDI beans with {@link AdditionalSecuredMethodsBuildItem}
 * if additional security is required. If you decide to use this build item, you must use
 * class security check storage and apply checks manually. Thus, it's only suitable for very special cases.
 */
public final class ClassSecurityCheckAnnotationBuildItem extends MultiBuildItem {

    private final DotName classAnnotation;

    /**
     * Quarkus will register security checks against {@link ClassSecurityCheckStorageBuildItem} for
     * classes annotated with the {@code classAnnotation} that are secured with a standard security annotation.
     *
     * @param classAnnotation class-level annotation name
     */
    public ClassSecurityCheckAnnotationBuildItem(DotName classAnnotation) {
        this.classAnnotation = Objects.requireNonNull(classAnnotation);
    }

    public DotName getClassAnnotation() {
        return classAnnotation;
    }
}
