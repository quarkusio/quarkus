package io.quarkus.keycloak.admin.rest.client.deployment.devservices;

import java.util.Map;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.devservices.keycloak.KeycloakAdminPageBuildItem;
import io.quarkus.devservices.keycloak.KeycloakDevServicesRequiredBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.keycloak.admin.client.common.deployment.KeycloakAdminClientInjectionEnabled;

@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = { DevServicesConfig.Enabled.class,
        KeycloakAdminClientInjectionEnabled.class })
public class KeycloakDevServiceRequiredBuildStep {

    private static final String SERVER_URL_CONFIG_KEY = "quarkus.keycloak.admin-client.server-url";

    @BuildStep
    KeycloakDevServicesRequiredBuildItem requireKeycloakDevService() {
        return KeycloakDevServicesRequiredBuildItem.of(
                ctx -> Map.of(SERVER_URL_CONFIG_KEY, ctx.authServerInternalBaseUrl()),
                SERVER_URL_CONFIG_KEY);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    KeycloakAdminPageBuildItem addCardWithLinkToKeycloakAdmin() {
        return new KeycloakAdminPageBuildItem(new CardPageBuildItem());
    }
}
