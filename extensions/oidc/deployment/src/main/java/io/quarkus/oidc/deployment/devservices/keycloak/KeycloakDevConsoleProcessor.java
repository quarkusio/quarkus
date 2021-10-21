package io.quarkus.oidc.deployment.devservices.keycloak;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.builditem.RuntimeConfigSetupCompleteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;
import io.quarkus.oidc.deployment.OidcBuildTimeConfig;
import io.quarkus.oidc.deployment.devservices.AbstractDevConsoleProcessor;
import io.quarkus.oidc.deployment.devservices.OidcAuthorizationCodePostHandler;
import io.quarkus.oidc.deployment.devservices.OidcPasswordClientCredHandler;
import io.quarkus.oidc.deployment.devservices.OidcTestServiceHandler;

public class KeycloakDevConsoleProcessor extends AbstractDevConsoleProcessor {

    KeycloakBuildTimeConfig keycloakConfig;
    OidcBuildTimeConfig oidcConfig;

    @BuildStep(onlyIf = IsDevelopment.class)
    @Consume(RuntimeConfigSetupCompleteBuildItem.class)
    public void setConfigProperties(BuildProducer<DevConsoleTemplateInfoBuildItem> console,
            Optional<KeycloakDevServicesConfigBuildItem> configProps,
            Capabilities capabilities) {
        if (configProps.isPresent() && configProps.get().getProperties().containsKey("keycloak.url")) {
            String keycloakUrl = (String) configProps.get().getProperties().get("keycloak.url");
            String realmUrl = keycloakUrl + "/realms/" + configProps.get().getProperties().get("keycloak.realm");

            console.produce(new DevConsoleTemplateInfoBuildItem("keycloakAdminUrl", keycloakUrl));
            console.produce(
                    new DevConsoleTemplateInfoBuildItem("keycloakUsers", configProps.get().getProperties().get("oidc.users")));

            produceDevConsoleTemplateItems(capabilities,
                    console,
                    "Keycloak",
                    (String) configProps.get().getProperties().get("quarkus.oidc.application-type"),
                    oidcConfig.devui.grant.type.isPresent() ? oidcConfig.devui.grant.type.get().getGrantType()
                            : keycloakConfig.devservices.grant.type.getGrantType(),
                    (String) configProps.get().getProperties().get("quarkus.oidc.client-id"),
                    (String) configProps.get().getProperties().get("quarkus.oidc.credentials.secret"),
                    realmUrl + "/protocol/openid-connect/auth",
                    realmUrl + "/protocol/openid-connect/token",
                    realmUrl + "/protocol/openid-connect/logout",
                    true);

        }
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    void invokeEndpoint(BuildProducer<DevConsoleRouteBuildItem> devConsoleRoute,
            Optional<KeycloakDevServicesConfigBuildItem> configProps) {
        if (configProps.isPresent() && configProps.get().getProperties().containsKey("keycloak.url")) {
            @SuppressWarnings("unchecked")
            Map<String, String> users = (Map<String, String>) configProps.get().getProperties().get("oidc.users");
            Duration webClientTimeout = oidcConfig.devui.webClienTimeout.isPresent() ? oidcConfig.devui.webClienTimeout.get()
                    : KeycloakDevServicesProcessor.capturedDevServicesConfiguration.webClienTimeout;
            produceDevConsoleRouteItems(devConsoleRoute,
                    new OidcTestServiceHandler(KeycloakDevServicesProcessor.vertxInstance, webClientTimeout),
                    new OidcAuthorizationCodePostHandler(KeycloakDevServicesProcessor.vertxInstance, webClientTimeout),
                    new OidcPasswordClientCredHandler(KeycloakDevServicesProcessor.vertxInstance, webClientTimeout, users));
        }
    }
}
