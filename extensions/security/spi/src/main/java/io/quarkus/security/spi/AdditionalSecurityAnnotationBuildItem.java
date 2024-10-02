package io.quarkus.security.spi;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Allows integrating extensions to signal they provide their own security annotation.
 * Standard security annotations cannot be combined and when two different annotations are applied,
 * one on the class level and second on the method level, the method level must win.
 * Without this build item, the Quarkus Security extension won't know about your security annotation integration.
 * Please beware that integrating extension-specific security annotation is responsibility of that extension.
 * This build item is intended for very specialized Quarkus core use cases, like integration of the authorization
 * policy in the Vert.x HTTP extension.
 */
public final class AdditionalSecurityAnnotationBuildItem extends MultiBuildItem {

    private final DotName securityAnnotationName;

    public AdditionalSecurityAnnotationBuildItem(DotName securityAnnotationName) {
        this.securityAnnotationName = securityAnnotationName;
    }

    public DotName getSecurityAnnotationName() {
        return securityAnnotationName;
    }
}
