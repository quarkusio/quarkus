package io.quarkus.jaxb.deployment;

import org.jboss.builder.item.MultiBuildItem;

/**
 * JAXB is an opt-in extension and must be enabled by
 * producing a <code>JaxbEnabledBuildItem</code>.
 */
public final class JaxbEnabledBuildItem extends MultiBuildItem {

    public JaxbEnabledBuildItem() {
    }

}
