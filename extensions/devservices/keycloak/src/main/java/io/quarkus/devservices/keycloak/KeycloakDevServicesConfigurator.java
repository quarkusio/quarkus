package io.quarkus.devservices.keycloak;

import java.util.Map;
import java.util.Set;

import org.keycloak.representations.idm.RealmRepresentation;

public interface KeycloakDevServicesConfigurator {

    interface ConfigPropertiesContext {

        String authServerInternalUrl();

        String oidcClientId();

        String oidcClientSecret();

        String authServerInternalBaseUrl();

        Map<String, String> generatedConfig();

    }

    Set<String> getLazyConfigKeys();

    String getLazyConfigValue(String configKey, ConfigPropertiesContext context);

    default void customizeDefaultRealm(RealmRepresentation realmRepresentation) {
    }

}
