package io.quarkus.devservices.keycloak;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.representations.idm.RealmRepresentation;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.devservices.keycloak.KeycloakDevServicesConfigurator.ConfigPropertiesContext;
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

    public record LazyConfigProperty(String configKey, Function<ConfigPropertiesContext, String> ctxToConfigValue) {

        public LazyConfigProperty(String configKey, String configValue) {
            this(configKey, ctx -> configValue);
        }

    }

    public static KeycloakDevServicesRequiredBuildItem of(Feature feature,
            LazyConfigProperty lazyConfigProperty, String authServerUrl, String... additionalDontStartConfigProperties) {
        return of(feature, List.of(lazyConfigProperty), authServerUrl, additionalDontStartConfigProperties);
    }

    public static KeycloakDevServicesRequiredBuildItem of(Feature feature,
            Collection<LazyConfigProperty> lazyConfigProperties,
            String authServerUrl, String... additionalDontStartConfigProperties) {
        var lazyConfigPropertiesCopy = List.copyOf(lazyConfigProperties);
        return of(feature, new KeycloakDevServicesConfigurator() {
            @Override
            public Set<String> getLazyConfigKeys() {
                return lazyConfigPropertiesCopy.stream().map(p -> p.configKey).collect(Collectors.toUnmodifiableSet());
            }

            @Override
            public String getLazyConfigValue(String configKey, ConfigPropertiesContext context) {
                return lazyConfigPropertiesCopy.stream()
                        .filter(p -> p.configKey.equals(configKey))
                        .map(p -> p.ctxToConfigValue.apply(context))
                        .filter(Objects::nonNull)
                        .filter(Predicate.not(String::isEmpty))
                        .findFirst()
                        .orElseThrow();
            }
        }, authServerUrl, additionalDontStartConfigProperties);
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
        return new KeycloakDevServicesConfigurator() {

            @Override
            public Set<String> getLazyConfigKeys() {
                return items.stream()
                        .flatMap(i -> i.devServicesConfigurator.getLazyConfigKeys().stream())
                        .collect(Collectors.toUnmodifiableSet());
            }

            @Override
            public String getLazyConfigValue(String configKey, ConfigPropertiesContext context) {
                return items.stream()
                        .map(i -> i.devServicesConfigurator)
                        .filter(c -> c.getLazyConfigKeys().contains(configKey))
                        .map(c -> c.getLazyConfigValue(configKey, context))
                        .filter(Objects::nonNull)
                        .filter(Predicate.not(String::isEmpty))
                        .findFirst()
                        .orElseThrow();
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
