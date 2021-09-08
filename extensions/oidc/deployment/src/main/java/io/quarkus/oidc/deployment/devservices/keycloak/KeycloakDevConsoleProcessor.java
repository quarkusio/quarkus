package io.quarkus.oidc.deployment.devservices.keycloak;

import java.util.Map;
import java.util.Optional;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.builditem.RuntimeConfigSetupCompleteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;
import io.quarkus.oidc.deployment.devservices.OidcAuthorizationCodePostHandler;
import io.quarkus.oidc.deployment.devservices.OidcTestServiceHandler;

public class KeycloakDevConsoleProcessor {

    KeycloakBuildTimeConfig config;

    @BuildStep(onlyIf = IsDevelopment.class)
    @Consume(RuntimeConfigSetupCompleteBuildItem.class)
    public void setConfigProperties(BuildProducer<DevConsoleTemplateInfoBuildItem> console,
            Optional<KeycloakDevServicesConfigBuildItem> configProps) {
        if (configProps.isPresent() && configProps.get().getProperties().containsKey("keycloak.url")) {
            String keycloakUrl = (String) configProps.get().getProperties().get("keycloak.url");
            String realmUrl = keycloakUrl + "/realms/" + configProps.get().getProperties().get("keycloak.realm");

            console.produce(new DevConsoleTemplateInfoBuildItem("keycloakUrl", keycloakUrl));
            console.produce(new DevConsoleTemplateInfoBuildItem("keycloakAdminUrl", keycloakUrl));
            console.produce(new DevConsoleTemplateInfoBuildItem("oidcApplicationType",
                    configProps.get().getProperties().get("quarkus.oidc.application-type")));
            console.produce(new DevConsoleTemplateInfoBuildItem("clientId",
                    configProps.get().getProperties().get("quarkus.oidc.client-id")));
            console.produce(new DevConsoleTemplateInfoBuildItem("clientSecret",
                    configProps.get().getProperties().get("quarkus.oidc.credentials.secret")));

            console.produce(
                    new DevConsoleTemplateInfoBuildItem("keycloakUsers", configProps.get().getProperties().get("oidc.users")));
            console.produce(new DevConsoleTemplateInfoBuildItem("tokenUrl", realmUrl + "/protocol/openid-connect/token"));
            console.produce(
                    new DevConsoleTemplateInfoBuildItem("authorizationUrl", realmUrl + "/protocol/openid-connect/auth"));
            console.produce(new DevConsoleTemplateInfoBuildItem("logoutUrl", realmUrl + "/protocol/openid-connect/logout"));
            console.produce(new DevConsoleTemplateInfoBuildItem("oidcGrantType", config.devservices.grant.type.getGrantType()));
        }
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    void invokeEndpoint(BuildProducer<DevConsoleRouteBuildItem> devConsoleRoute,
            Optional<KeycloakDevServicesConfigBuildItem> configProps) {
        if (configProps.isPresent() && configProps.get().getProperties().containsKey("keycloak.url")) {
            @SuppressWarnings("unchecked")
            Map<String, String> users = (Map<String, String>) configProps.get().getProperties().get("oidc.users");
            devConsoleRoute.produce(
                    new DevConsoleRouteBuildItem("testService", "POST", new KeycloakDevConsolePostHandler(users)));
            devConsoleRoute.produce(
                    new DevConsoleRouteBuildItem("testServiceWithToken", "POST",
                            new OidcTestServiceHandler(KeycloakDevServicesProcessor.vertxInstance,
                                    KeycloakDevServicesProcessor.capturedDevServicesConfiguration.webClienTimeout)));
            devConsoleRoute.produce(
                    new DevConsoleRouteBuildItem("exchangeCodeForTokens", "POST",
                            new OidcAuthorizationCodePostHandler(KeycloakDevServicesProcessor.vertxInstance,
                                    KeycloakDevServicesProcessor.capturedDevServicesConfiguration.webClienTimeout)));
        }
    }
}
