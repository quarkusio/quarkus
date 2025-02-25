package io.quarkus.devservices.keycloak;

import java.util.Map;

import org.keycloak.representations.idm.RealmRepresentation;

public interface KeycloakDevServicesConfigurator {

    record ConfigPropertiesContext(String authServerInternalUrl, String oidcClientId, String oidcClientSecret,
            String authServerInternalBaseUrl) {
    }

    Map<String, String> createProperties(ConfigPropertiesContext context);

    default void customizeDefaultRealm(RealmRepresentation realmRepresentation) {
    }

}
