package io.quarkus.devservices.keycloak;

import static io.quarkus.devservices.keycloak.KeycloakDevServicesProcessor.OIDC_CLIENT_AUTH_SERVER_URL_CONFIG_KEY;
import static io.quarkus.devservices.keycloak.KeycloakDevServicesProcessor.OIDC_CLIENT_TOKEN_PATH_CONFIG_KEY;

import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.configuration.ConfigUtils;

/**
 * A marker build item signifying that integrating extensions (like OIDC and OIDC client)
 * are enabled. The Keycloak Dev Service will be started in DEV mode if at least one item is produced
 * and the Dev Service is not disabled in other fashion.
 */
public final class KeycloakDevServicesRequiredBuildItem extends MultiBuildItem {

    enum Capability {
        OIDC,
        OIDC_CLIENT
    }

    private final Capability capability;

    private KeycloakDevServicesRequiredBuildItem(Capability capability) {
        this.capability = capability;
    }

    static boolean setOidcConfigProperties(List<KeycloakDevServicesRequiredBuildItem> items) {
        return items.stream().anyMatch(i -> i.capability == Capability.OIDC);
    }

    static boolean setOidcClientConfigProperties(List<KeycloakDevServicesRequiredBuildItem> items) {
        boolean serverUrlOrTokenPathConfigured = ConfigUtils.isPropertyNonEmpty(OIDC_CLIENT_AUTH_SERVER_URL_CONFIG_KEY)
                || ConfigUtils.isPropertyNonEmpty(OIDC_CLIENT_TOKEN_PATH_CONFIG_KEY);
        return !serverUrlOrTokenPathConfigured
                && items.stream().anyMatch(i -> i.capability == Capability.OIDC_CLIENT);
    }

    public static KeycloakDevServicesRequiredBuildItem requireDevServiceForOidc() {
        return new KeycloakDevServicesRequiredBuildItem(Capability.OIDC);
    }

    public static KeycloakDevServicesRequiredBuildItem requireDevServiceForOidcClient() {
        return new KeycloakDevServicesRequiredBuildItem(Capability.OIDC_CLIENT);
    }
}
