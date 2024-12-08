package io.quarkus.devservices.keycloak;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.representations.idm.RealmRepresentation;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.configuration.ConfigUtils;

/**
 * A marker build item signifying that integrating extensions (like OIDC and OIDC client)
 * are enabled. The Keycloak Dev Service will be started in DEV mode if at least one item is produced
 * and the Dev Service is not disabled in other fashion.
 */
public final class KeycloakDevServicesRequiredBuildItem extends MultiBuildItem {

    private static final Logger LOG = Logger.getLogger(KeycloakDevServicesProcessor.class);
    public static final String OIDC_AUTH_SERVER_URL_CONFIG_KEY = "quarkus.oidc.auth-server-url";
    private static final String OIDC_PROVIDER_CONFIG_KEY = "quarkus.oidc.provider";

    private final KeycloakDevServicesConfigurator devServicesConfigurator;
    private final String authServerUrl;

    private KeycloakDevServicesRequiredBuildItem(KeycloakDevServicesConfigurator devServicesConfigurator,
            String authServerUrl) {
        this.devServicesConfigurator = requireNonNull(devServicesConfigurator);
        this.authServerUrl = requireNonNull(authServerUrl);
    }

    String getAuthServerUrl() {
        return authServerUrl;
    }

    public static KeycloakDevServicesRequiredBuildItem of(KeycloakDevServicesConfigurator devServicesConfigurator,
            String authServerUrl, String... additionalDontStartConfigProperties) {
        final Set<String> dontStartConfigProperties = new HashSet<>(Arrays.asList(additionalDontStartConfigProperties));
        dontStartConfigProperties.add(authServerUrl);
        dontStartConfigProperties.add(OIDC_AUTH_SERVER_URL_CONFIG_KEY);
        dontStartConfigProperties.add(OIDC_PROVIDER_CONFIG_KEY);
        return of(devServicesConfigurator, authServerUrl, dontStartConfigProperties);
    }

    private static KeycloakDevServicesRequiredBuildItem of(KeycloakDevServicesConfigurator devServicesConfigurator,
            String authServerUrl, Set<String> dontStartConfigProperties) {
        if (shouldStartDevService(dontStartConfigProperties)) {
            return new KeycloakDevServicesRequiredBuildItem(devServicesConfigurator, authServerUrl);
        }
        return null;
    }

    static KeycloakDevServicesConfigurator getDevServicesConfigurator(List<KeycloakDevServicesRequiredBuildItem> items) {
        return new KeycloakDevServicesConfigurator() {
            @Override
            public Map<String, String> createProperties(ConfigPropertiesContext context) {
                return items
                        .stream()
                        .map(i -> i.devServicesConfigurator)
                        .map(producer -> producer.createProperties(context))
                        .map(Map::entrySet)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }

            @Override
            public void customizeDefaultRealm(RealmRepresentation realmRepresentation) {
                items
                        .stream()
                        .map(i -> i.devServicesConfigurator)
                        .forEach(i -> i.customizeDefaultRealm(realmRepresentation));
            }
        };
    }

    private static boolean shouldStartDevService(Set<String> dontStartConfigProperties) {
        return dontStartConfigProperties.stream().allMatch(KeycloakDevServicesRequiredBuildItem::shouldStartDevService);
    }

    private static boolean shouldStartDevService(String dontStartConfigProperty) {
        if (ConfigUtils.isPropertyNonEmpty(dontStartConfigProperty)) {
            // this build item does not require to start the Keycloak Dev Service as runtime property was set
            LOG.debugf("Not starting Dev Services for Keycloak as '%s' has been provided", dontStartConfigProperty);
            return false;
        }
        return true;
    }
}
