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

public class KeycloakDevConsoleProcessor {

    KeycloakBuildTimeConfig config;

    @BuildStep(onlyIf = IsDevelopment.class)
    @Consume(RuntimeConfigSetupCompleteBuildItem.class)
    public void setConfigProperties(BuildProducer<DevConsoleTemplateInfoBuildItem> console,
            Optional<KeycloakDevServicesConfigBuildItem> configProps) {
        if (configProps.isPresent()) {
            console.produce(
                    new DevConsoleTemplateInfoBuildItem("devServicesEnabled", config.devservices.enabled));
            console.produce(
                    new DevConsoleTemplateInfoBuildItem("keycloakUrl", configProps.get().getProperties().get("keycloak.url")));
            console.produce(new DevConsoleTemplateInfoBuildItem("oidcApplicationType",
                    configProps.get().getProperties().get("quarkus.oidc.application-type")));
            console.produce(new DevConsoleTemplateInfoBuildItem("keycloakClient",
                    configProps.get().getProperties().get("quarkus.oidc.client-id")));
            console.produce(new DevConsoleTemplateInfoBuildItem("keycloakClientSecret",
                    configProps.get().getProperties().get("quarkus.oidc.credentials.secret")));

            console.produce(
                    new DevConsoleTemplateInfoBuildItem("keycloakUsers", configProps.get().getProperties().get("oidc.users")));
            console.produce(new DevConsoleTemplateInfoBuildItem("keycloakRealm", config.devservices.realmName));
            console.produce(new DevConsoleTemplateInfoBuildItem("oidcGrantType", config.devservices.grant.type.getGrantType()));
        }
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    void invokeEndpoint(BuildProducer<DevConsoleRouteBuildItem> devConsoleRoute,
            Optional<KeycloakDevServicesConfigBuildItem> configProps) {
        if (configProps.isPresent()) {
            @SuppressWarnings("unchecked")
            Map<String, String> users = (Map<String, String>) configProps.get().getProperties().get("oidc.users");
            devConsoleRoute.produce(
                    new DevConsoleRouteBuildItem("testService", "POST", new KeycloakDevConsolePostHandler(users)));
            devConsoleRoute.produce(
                    new DevConsoleRouteBuildItem("testServiceWithToken", "POST", new KeycloakImplicitGrantPostHandler()));
            devConsoleRoute.produce(
                    new DevConsoleRouteBuildItem("exchangeCodeForTokens", "POST", new KeycloakAuthorizationCodePostHandler()));
        }
    }
}
