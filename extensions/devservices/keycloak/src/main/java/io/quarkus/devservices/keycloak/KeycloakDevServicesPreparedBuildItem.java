package io.quarkus.devservices.keycloak;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;

/**
 * Marker used by Build Steps that perform tasks which must run after Keycloak Dev Services was prepared
 * (the {@link DevServicesResultBuildItem} was created). If the Keycloak Dev Service did not start, this item
 * won't be produced.
 */
public final class KeycloakDevServicesPreparedBuildItem extends SimpleBuildItem {

    private final String devServiceConfigHashCode;

    KeycloakDevServicesPreparedBuildItem(String devServiceConfigHashCode) {
        this.devServiceConfigHashCode = devServiceConfigHashCode;
    }

    public String getDevServiceConfigHashCode() {
        return devServiceConfigHashCode;
    }
}
