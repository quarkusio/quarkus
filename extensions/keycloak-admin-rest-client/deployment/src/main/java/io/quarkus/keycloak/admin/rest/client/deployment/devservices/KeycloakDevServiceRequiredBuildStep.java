package io.quarkus.keycloak.admin.rest.client.deployment.devservices;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.devservices.keycloak.KeycloakAdminPageBuildItem;
import io.quarkus.devservices.keycloak.KeycloakDevServicesConfigurator.ConfigPropertiesContext;
import io.quarkus.devservices.keycloak.KeycloakDevServicesConfigurator.LazyConfigProperty;
import io.quarkus.devservices.keycloak.KeycloakDevServicesRequiredBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.keycloak.admin.client.common.deployment.KeycloakAdminClientInjectionEnabled;

@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class,
        KeycloakAdminClientInjectionEnabled.class })
public class KeycloakDevServiceRequiredBuildStep {

    private static final String SERVER_URL_CONFIG_KEY = "quarkus.keycloak.admin-client.server-url";

    @BuildStep
    KeycloakDevServicesRequiredBuildItem requireKeycloakDevService() {
        // TODO: introduce Keycloak Admin Client feature, I just don't want to do it in this already large PR
        return KeycloakDevServicesRequiredBuildItem.of(Feature.OIDC,
                new LazyConfigProperty(SERVER_URL_CONFIG_KEY, ConfigPropertiesContext::authServerInternalBaseUrl),
                SERVER_URL_CONFIG_KEY);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    KeycloakAdminPageBuildItem addCardWithLinkToKeycloakAdmin() {
        return new KeycloakAdminPageBuildItem(new CardPageBuildItem());
    }
}
