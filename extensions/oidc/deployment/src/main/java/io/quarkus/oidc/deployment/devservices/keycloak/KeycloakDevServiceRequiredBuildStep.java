package io.quarkus.oidc.deployment.devservices.keycloak;

import static io.quarkus.devservices.keycloak.KeycloakDevServicesRequiredBuildItem.OIDC_AUTH_SERVER_URL_CONFIG_KEY;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.devservices.keycloak.KeycloakDevServicesConfig;
import io.quarkus.devservices.keycloak.KeycloakDevServicesConfigurator.ConfigPropertiesContext;
import io.quarkus.devservices.keycloak.KeycloakDevServicesConfigurator.LazyConfigProperty;
import io.quarkus.devservices.keycloak.KeycloakDevServicesRequiredBuildItem;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig.Credentials.Jwt.Source;
import io.quarkus.oidc.deployment.OidcBuildStep;

@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, OidcBuildStep.IsEnabled.class,
        DevServicesConfig.Enabled.class })
public class KeycloakDevServiceRequiredBuildStep {

    private static final Logger LOG = Logger.getLogger(KeycloakDevServiceRequiredBuildStep.class);
    private static final String CONFIG_PREFIX = "quarkus.oidc.";
    private static final String TENANT_ENABLED_CONFIG_KEY = CONFIG_PREFIX + "tenant-enabled";
    private static final String CLIENT_ID_CONFIG_KEY = CONFIG_PREFIX + "client-id";
    private static final String CLIENT_SECRET_CONFIG_KEY = CONFIG_PREFIX + "credentials.secret";
    private static final String JWT_SOURCE_CONFIG_KEY = CONFIG_PREFIX + "credentials.jwt.source";

    @BuildStep
    KeycloakDevServicesRequiredBuildItem requireKeycloakDevService(KeycloakDevServicesConfig config) {
        if (!isOidcTenantEnabled() && !config.startWithDisabledTenant()) {
            LOG.debug("Not starting Dev Services for Keycloak as 'quarkus.oidc.tenant.enabled' is false");
            return null;
        }

        final Collection<LazyConfigProperty> lazyConfigProperties = new LinkedList<>(List.of(
                new LazyConfigProperty(OIDC_AUTH_SERVER_URL_CONFIG_KEY, ConfigPropertiesContext::authServerInternalUrl)));
        if (config.createClient()) {
            lazyConfigProperties.add(new LazyConfigProperty(CLIENT_ID_CONFIG_KEY, ConfigPropertiesContext::oidcClientId));
            Source jwtSource = ConfigProvider.getConfig().getOptionalValue(JWT_SOURCE_CONFIG_KEY, Source.class)
                    .orElse(Source.CLIENT);
            if (jwtSource == Source.CLIENT) {
                lazyConfigProperties
                        .add(new LazyConfigProperty(CLIENT_SECRET_CONFIG_KEY, ConfigPropertiesContext::oidcClientSecret));
            }
        }
        return KeycloakDevServicesRequiredBuildItem.of(Feature.OIDC, lazyConfigProperties, OIDC_AUTH_SERVER_URL_CONFIG_KEY);
    }

    private static boolean isOidcTenantEnabled() {
        return ConfigProvider.getConfig().getOptionalValue(TENANT_ENABLED_CONFIG_KEY, Boolean.class).orElse(true);
    }
}
