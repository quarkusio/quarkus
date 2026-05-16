package io.quarkus.devservices.keycloak;

import java.util.Optional;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;

/**
 * Marker used by Build Steps that perform tasks which must run after Keycloak Dev Services was prepared
 * (the {@link DevServicesResultBuildItem} was created). If the Keycloak Dev Service did not start, this item
 * won't be produced.
 */
public final class KeycloakDevServicesPreparedBuildItem extends SimpleBuildItem {

    private final String devServiceConfigHashCode;
    private final boolean realmExportSupported;

    KeycloakDevServicesPreparedBuildItem(String devServiceConfigHashCode, boolean realmExportSupported) {
        this.devServiceConfigHashCode = devServiceConfigHashCode;
        this.realmExportSupported = realmExportSupported;
    }

    public String getDevServiceConfigHashCode() {
        return devServiceConfigHashCode;
    }

    static boolean isRealmExportSupported(Optional<KeycloakDevServicesPreparedBuildItem> optionalSelf) {
        return optionalSelf.isPresent() && optionalSelf.get().realmExportSupported;
    }

    static boolean isRealmExportNotSupported(Optional<KeycloakDevServicesPreparedBuildItem> optionalSelf) {
        return !isRealmExportSupported(optionalSelf);
    }
}
