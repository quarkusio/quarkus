package io.quarkus.elytron.security.deployment;

import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.undertow.security.idm.IdentityManager;

/**
 * Used to identify which {@linkplain IdentityManager} to install in the deployment. Even though this is a MultiBuildItem,
 * only one can be created across security related extensions. If more than one is created, the
 * {@linkplain SecurityDeploymentProcessor#addIdentityManager(SecurityRecorder, BuildProducer, SecurityDomainBuildItem, List, List)}
 * will throw an IllegalStateException.
 */
public final class IdentityManagerBuildItem extends MultiBuildItem {

    private final IdentityManager identityManager;

    public IdentityManagerBuildItem(IdentityManager identityManager) {
        this.identityManager = identityManager;
    }

    public IdentityManager getIdentityManager() {
        return identityManager;
    }
}
