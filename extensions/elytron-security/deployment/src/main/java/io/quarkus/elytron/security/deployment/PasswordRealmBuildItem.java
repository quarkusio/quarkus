package io.quarkus.elytron.security.deployment;

import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;

/**
 * Marker build item indicating that a password based realm was installed.
 *
 * @see SecurityDeploymentProcessor#configureIdentityManager(SecurityRecorder, SecurityDomainBuildItem, BuildProducer, List)
 */
public final class PasswordRealmBuildItem extends MultiBuildItem {
    public PasswordRealmBuildItem() {
    }
}
