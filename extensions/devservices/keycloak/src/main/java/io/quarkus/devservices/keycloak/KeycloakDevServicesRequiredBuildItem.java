package io.quarkus.devservices.keycloak;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.devservices.keycloak.KeycloakDevServicesConfigurator.ConfigPropertiesContext;
import io.quarkus.devservices.keycloak.KeycloakDevServicesConfigurator.LazyConfigKeycloakDevServicesConfigurator;
import io.quarkus.devservices.keycloak.KeycloakDevServicesConfigurator.LazyConfigProperty;
import io.quarkus.runtime.configuration.ConfigUtils;

/**
 * A marker build item signifying that integrating extensions (like OIDC and OIDC client)
 * are enabled. The Keycloak Dev Service will be started in DEV mode if at least one item is produced
 * and the Dev Service is not disabled in other fashion.
 */
public final class KeycloakDevServicesRequiredBuildItem extends MultiBuildItem {

    private static final String CONFIG_PREFIX = "quarkus.oidc.";
    private static final Logger LOG = Logger.getLogger(KeycloakDevServicesProcessor.class);
    private static final String OIDC_PROVIDER_CONFIG_KEY = CONFIG_PREFIX + "provider";
    // avoid the Quarkus prefix in order to prevent warnings when the application starts in container integration tests
    private static final String CLIENT_AUTH_SERVER_URL_CONFIG_KEY = "client." + CONFIG_PREFIX + "auth-server-url";
    private static final String OIDC_USERS = "oidc.users";
    private static final String KEYCLOAK_REALMS = "keycloak.realms";
    private static final String KEYCLOAK_URL_KEY = "keycloak.url";
    // the authentication server URL required by the OIDC DEV UI
    private static final String KEYCLOAK_AUTH_SERVER_INTERNAL_URL = "keycloak.auth-server-internal-url";
    public static final String OIDC_AUTH_SERVER_URL_CONFIG_KEY = CONFIG_PREFIX + "auth-server-url";

    private final KeycloakDevServicesConfigurator devServicesConfigurator;
    private final String authServerUrl;
    private final Feature feature;

    private KeycloakDevServicesRequiredBuildItem(KeycloakDevServicesConfigurator devServicesConfigurator,
            String authServerUrl, Feature feature) {
        this.devServicesConfigurator = requireNonNull(devServicesConfigurator);
        this.authServerUrl = requireNonNull(authServerUrl);
        this.feature = feature;
    }

    String getAuthServerUrl() {
        return authServerUrl;
    }

    public static KeycloakDevServicesRequiredBuildItem of(Feature feature,
            LazyConfigProperty lazyConfigProperty, String authServerUrl, String... additionalDontStartConfigProperties) {
        return of(feature, List.of(lazyConfigProperty), authServerUrl, additionalDontStartConfigProperties);
    }

    public static KeycloakDevServicesRequiredBuildItem of(Feature feature,
            Collection<LazyConfigProperty> lazyConfigProperties,
            String authServerUrl, String... additionalDontStartConfigProperties) {
        var lazyConfigPropertiesCopy = List.copyOf(lazyConfigProperties);
        return of(feature, new LazyConfigKeycloakDevServicesConfigurator(lazyConfigPropertiesCopy), authServerUrl,
                additionalDontStartConfigProperties);
    }

    public static KeycloakDevServicesRequiredBuildItem of(Feature feature,
            KeycloakDevServicesConfigurator devServicesConfigurator,
            String authServerUrl, String... additionalDontStartConfigProperties) {
        final Set<String> dontStartConfigProperties = new HashSet<>(Arrays.asList(additionalDontStartConfigProperties));
        dontStartConfigProperties.add(authServerUrl);
        dontStartConfigProperties.add(OIDC_AUTH_SERVER_URL_CONFIG_KEY);
        dontStartConfigProperties.add(OIDC_PROVIDER_CONFIG_KEY);
        return of(devServicesConfigurator, authServerUrl, dontStartConfigProperties, feature);
    }

    private static KeycloakDevServicesRequiredBuildItem of(KeycloakDevServicesConfigurator devServicesConfigurator,
            String authServerUrl, Set<String> dontStartConfigProperties, Feature feature) {
        if (shouldStartDevService(dontStartConfigProperties)) {
            return new KeycloakDevServicesRequiredBuildItem(devServicesConfigurator, authServerUrl, feature);
        }
        return null;
    }

    static KeycloakDevServicesConfigurator getDevServicesConfigurator(List<KeycloakDevServicesRequiredBuildItem> items) {
        KeycloakDevServicesConfigurator baseDevServicesConfigurator = new LazyConfigKeycloakDevServicesConfigurator(List.of(
                new LazyConfigProperty(CLIENT_AUTH_SERVER_URL_CONFIG_KEY, ConfigPropertiesContext::clientAuthServerUrl),
                new LazyConfigProperty(KEYCLOAK_URL_KEY, ConfigPropertiesContext::keycloakUrl),
                new LazyConfigProperty(OIDC_USERS, ConfigPropertiesContext::oidcUsers),
                new LazyConfigProperty(KEYCLOAK_AUTH_SERVER_INTERNAL_URL, ConfigPropertiesContext::authServerInternalUrl),
                new LazyConfigProperty(KEYCLOAK_REALMS, ConfigPropertiesContext::keycloakRealms)));
        List<KeycloakDevServicesConfigurator> configurators = Stream.concat(Stream.of(baseDevServicesConfigurator),
                items.stream().map(i -> i.devServicesConfigurator)).toList();
        return new KeycloakDevServicesConfigurator.ComposedKeycloakDevServicesConfigurator(configurators);
    }

    /**
     * @return the feature for which the dev service should be created; if there is multiple features, prefer OIDC
     *         as it covers all the rest
     */
    static Feature getFeature(List<KeycloakDevServicesRequiredBuildItem> devSvcRequiredMarkerItems) {
        Feature feature = null;
        for (KeycloakDevServicesRequiredBuildItem devSvcRequiredMarkerItem : devSvcRequiredMarkerItems) {
            if (feature == null) {
                feature = devSvcRequiredMarkerItem.feature;
            } else if (devSvcRequiredMarkerItem.feature == Feature.OIDC) {
                feature = Feature.OIDC;
                break;
            }
        }
        return feature;
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
