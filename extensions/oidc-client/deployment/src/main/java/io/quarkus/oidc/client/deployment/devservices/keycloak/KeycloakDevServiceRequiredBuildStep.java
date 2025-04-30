package io.quarkus.oidc.client.deployment.devservices.keycloak;

import java.util.HashMap;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.devservices.keycloak.KeycloakAdminPageBuildItem;
import io.quarkus.devservices.keycloak.KeycloakDevServicesConfig;
import io.quarkus.devservices.keycloak.KeycloakDevServicesRequiredBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.oidc.client.deployment.OidcClientBuildStep;

@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = { OidcClientBuildStep.IsEnabled.class, DevServicesConfig.Enabled.class })
public class KeycloakDevServiceRequiredBuildStep {

    private static final String CONFIG_PREFIX = "quarkus.oidc-client.";
    private static final String OIDC_CLIENT_AUTH_SERVER_URL_CONFIG_KEY = CONFIG_PREFIX + "auth-server-url";
    private static final String OIDC_CLIENT_TOKEN_PATH_CONFIG_KEY = CONFIG_PREFIX + "token-path";
    private static final String OIDC_CLIENT_SECRET_CONFIG_KEY = CONFIG_PREFIX + "credentials.secret";
    private static final String OIDC_CLIENT_ID_CONFIG_KEY = CONFIG_PREFIX + "client-id";

    @BuildStep
    KeycloakDevServicesRequiredBuildItem requireKeycloakDevService(KeycloakDevServicesConfig config) {
        return KeycloakDevServicesRequiredBuildItem.of(ctx -> {
            var configProperties = new HashMap<String, String>();
            configProperties.put(OIDC_CLIENT_AUTH_SERVER_URL_CONFIG_KEY, ctx.authServerInternalUrl());
            configProperties.put(OIDC_CLIENT_TOKEN_PATH_CONFIG_KEY, "/protocol/openid-connect/tokens");
            if (config.createClient()) {
                configProperties.put(OIDC_CLIENT_ID_CONFIG_KEY, ctx.oidcClientId());
                configProperties.put(OIDC_CLIENT_SECRET_CONFIG_KEY, ctx.oidcClientSecret());
            }
            return configProperties;
        }, OIDC_CLIENT_AUTH_SERVER_URL_CONFIG_KEY, OIDC_CLIENT_TOKEN_PATH_CONFIG_KEY);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    KeycloakAdminPageBuildItem addCardWithLinkToKeycloakAdmin() {
        return new KeycloakAdminPageBuildItem(new CardPageBuildItem());
    }
}
