package io.quarkus.oidc.client.registration.deployment.devservices.keycloak;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.ConfigProvider;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.devservices.keycloak.KeycloakAdminPageBuildItem;
import io.quarkus.devservices.keycloak.KeycloakDevServicesConfigurator;
import io.quarkus.devservices.keycloak.KeycloakDevServicesRequiredBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.oidc.client.registration.deployment.OidcClientRegistrationBuildStep;

@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = { OidcClientRegistrationBuildStep.IsEnabled.class,
        DevServicesConfig.Enabled.class })
public class KeycloakDevServiceRequiredBuildStep {

    private static final String OIDC_CLIENT_REG_AUTH_SERVER_URL_CONFIG_KEY = "quarkus.oidc-client-registration.auth-server-url";

    @BuildStep
    KeycloakDevServicesRequiredBuildItem requireKeycloakDevService() {
        var devServicesConfigurator = new KeycloakDevServicesConfigurator() {

            @Override
            public Map<String, String> createProperties(ConfigPropertiesContext ctx) {
                return Map.of(OIDC_CLIENT_REG_AUTH_SERVER_URL_CONFIG_KEY, ctx.authServerInternalUrl());
            }

            @Override
            public void customizeDefaultRealm(RealmRepresentation realmRepresentation) {
                if (getInitialToken() == null) {
                    realmRepresentation.setRegistrationAllowed(true);
                    realmRepresentation.setRegistrationFlow("registration");
                    if (realmRepresentation.getComponents() == null) {
                        realmRepresentation.setComponents(new MultivaluedHashMap<>());
                    }
                    var componentExportRepresentation = new ComponentExportRepresentation();
                    componentExportRepresentation.setName("Full Scope Disabled");
                    componentExportRepresentation.setProviderId("scope");
                    componentExportRepresentation.setSubType("anonymous");
                    realmRepresentation.getComponents().put(
                            "org.keycloak.services.clientregistration.policy.ClientRegistrationPolicy",
                            List.of(componentExportRepresentation));
                }
            }
        };

        return KeycloakDevServicesRequiredBuildItem.of(devServicesConfigurator, OIDC_CLIENT_REG_AUTH_SERVER_URL_CONFIG_KEY);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    KeycloakAdminPageBuildItem addCardWithLinkToKeycloakAdmin() {
        return new KeycloakAdminPageBuildItem(new CardPageBuildItem());
    }

    private static String getInitialToken() {
        return ConfigProvider.getConfig().getOptionalValue("quarkus.oidc-client-registration.initial-token", String.class)
                .orElse(null);
    }
}
