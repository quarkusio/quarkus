package io.quarkus.security.spi;

import java.util.Objects;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.security.identity.CurrentIdentityAssociation;

/**
 * Allows Quarkus core extensions to provide a {@link io.quarkus.security.identity.CurrentIdentityAssociation} implementation.
 * This bean is used to synchronize the association produced by Quarkus core extensions and can change if needed.
 * Other extensions and users can simply define alternative {@link io.quarkus.security.identity.CurrentIdentityAssociation}
 * CDI bean.
 */
public final class CurrentIdentityAssociationClassBuildItem extends SimpleBuildItem {

    private final Class<? extends CurrentIdentityAssociation> currentIdentityAssociationClass;

    public CurrentIdentityAssociationClassBuildItem(
            Class<? extends CurrentIdentityAssociation> currentIdentityAssociationClass) {
        this.currentIdentityAssociationClass = Objects.requireNonNull(currentIdentityAssociationClass);
    }

    public Class<? extends CurrentIdentityAssociation> getCurrentIdentityAssociationClass() {
        return currentIdentityAssociationClass;
    }
}
