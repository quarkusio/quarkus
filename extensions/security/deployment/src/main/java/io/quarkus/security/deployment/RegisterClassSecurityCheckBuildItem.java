package io.quarkus.security.deployment;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Registers security check against {@link io.quarkus.security.spi.ClassSecurityCheckStorageBuildItem}
 * for security annotation instances passed in this build item.
 */
final class RegisterClassSecurityCheckBuildItem extends MultiBuildItem {

    final DotName className;
    final AnnotationInstance securityAnnotationInstance;

    RegisterClassSecurityCheckBuildItem(DotName className, AnnotationInstance securityAnnotationInstance) {
        this.className = className;
        this.securityAnnotationInstance = securityAnnotationInstance;
    }
}
