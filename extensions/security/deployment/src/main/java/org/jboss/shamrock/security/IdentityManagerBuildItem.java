package org.jboss.shamrock.security;

import java.util.List;

import io.undertow.security.idm.IdentityManager;
import org.jboss.builder.item.MultiBuildItem;
import org.jboss.shamrock.deployment.annotations.BuildProducer;
import org.jboss.shamrock.runtime.RuntimeValue;

/**
 * Used to identify which {@linkplain IdentityManager} to install in the deployment. Even though this is a MultiBuildItem,
 * only one can be created across security related extensions. If more than one is created, the
 * {@linkplain SecurityDeploymentProcessor#addIdentityManager(SecurityTemplate, BuildProducer, SecurityDomainBuildItem, List, List)}
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
