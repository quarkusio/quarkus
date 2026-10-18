package io.quarkus.security.spi;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Registers security check against {@link io.quarkus.security.spi.ClassSecurityCheckStorageBuildItem}
 * for security annotation instances passed in this build item.
 * This class is exposed for limited Quarkus core-specific use cases and can be changed or be removed if necessary.
 * If other extensions require this build item, please open Quarkus issue so that we document and test the use case.
 */
public final class RegisterClassSecurityCheckBuildItem extends MultiBuildItem {

    private final DotName className;
    private final AnnotationInstance securityAnnotationInstance;

    public RegisterClassSecurityCheckBuildItem(DotName className, AnnotationInstance securityAnnotationInstance) {
        this.className = className;
        this.securityAnnotationInstance = securityAnnotationInstance;
    }

    public DotName getClassName() {
        return className;
    }

    public AnnotationInstance getSecurityAnnotationInstance() {
        return securityAnnotationInstance;
    }
}
