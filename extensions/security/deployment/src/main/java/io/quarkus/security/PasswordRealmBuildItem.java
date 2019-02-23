package io.quarkus.security;

import java.util.List;

import org.jboss.builder.item.MultiBuildItem;

import io.quarkus.deployment.annotations.BuildProducer;

/**
 * Marker build item indicating that a password based realm was installed.
 * 
 * @see SecurityDeploymentProcessor#configureIdentityManager(SecurityTemplate, SecurityDomainBuildItem, BuildProducer, List)
 */
public final class PasswordRealmBuildItem extends MultiBuildItem {
    public PasswordRealmBuildItem() {
    }
}
