package io.quarkus.oidc.client.deployment.devservices.keycloak;

import java.util.Collection;
import java.util.List;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.devservices.keycloak.KeycloakAdminPageBuildItem;
import io.quarkus.devservices.keycloak.KeycloakDevServicesConfig;
import io.quarkus.devservices.keycloak.KeycloakDevServicesConfigurator.ConfigPropertiesContext;
import io.quarkus.devservices.keycloak.KeycloakDevServicesConfigurator.LazyConfigProperty;
import io.quarkus.devservices.keycloak.KeycloakDevServicesRequiredBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.oidc.client.deployment.OidcClientBuildStep;

@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, OidcClientBuildStep.IsEnabled.class,
        DevServicesConfig.Enabled.class })
public class KeycloakDevServiceRequiredBuildStep {

    private static final String CONFIG_PREFIX = "quarkus.oidc-client.";
    private static final String OIDC_CLIENT_AUTH_SERVER_URL_CONFIG_KEY = CONFIG_PREFIX + "auth-server-url";
    private static final String OIDC_CLIENT_TOKEN_PATH_CONFIG_KEY = CONFIG_PREFIX + "token-path";
    private static final String OIDC_CLIENT_SECRET_CONFIG_KEY = CONFIG_PREFIX + "credentials.secret";
    private static final String OIDC_CLIENT_ID_CONFIG_KEY = CONFIG_PREFIX + "client-id";

    @BuildStep
    KeycloakDevServicesRequiredBuildItem requireKeycloakDevService(KeycloakDevServicesConfig config) {
        final Collection<LazyConfigProperty> lazyConfigProperties;
        if (config.createClient()) {
            lazyConfigProperties = List.of(
                    new LazyConfigProperty(OIDC_CLIENT_AUTH_SERVER_URL_CONFIG_KEY,
                            ConfigPropertiesContext::authServerInternalUrl),
                    new LazyConfigProperty(OIDC_CLIENT_TOKEN_PATH_CONFIG_KEY, "/protocol/openid-connect/tokens"),
                    new LazyConfigProperty(OIDC_CLIENT_ID_CONFIG_KEY, ConfigPropertiesContext::oidcClientId),
                    new LazyConfigProperty(OIDC_CLIENT_SECRET_CONFIG_KEY, ConfigPropertiesContext::oidcClientSecret));
        } else {
            lazyConfigProperties = List.of(
                    new LazyConfigProperty(OIDC_CLIENT_AUTH_SERVER_URL_CONFIG_KEY,
                            ConfigPropertiesContext::authServerInternalUrl),
                    new LazyConfigProperty(OIDC_CLIENT_TOKEN_PATH_CONFIG_KEY, "/protocol/openid-connect/tokens"));
        }
        return KeycloakDevServicesRequiredBuildItem.of(Feature.OIDC_CLIENT, lazyConfigProperties,
                OIDC_CLIENT_AUTH_SERVER_URL_CONFIG_KEY, OIDC_CLIENT_TOKEN_PATH_CONFIG_KEY);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    KeycloakAdminPageBuildItem addCardWithLinkToKeycloakAdmin() {
        return new KeycloakAdminPageBuildItem(new CardPageBuildItem());
    }
}
