package io.quarkus.oidc.deployment.devservices.keycloak;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ConfigurationBuildItem;
import io.quarkus.deployment.builditem.RuntimeConfigSetupCompleteBuildItem;
import io.quarkus.devservices.keycloak.KeycloakAdminPageBuildItem;
import io.quarkus.devservices.keycloak.KeycloakDevServicesConfigBuildItem;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.oidc.deployment.DevUiConfig;
import io.quarkus.oidc.deployment.OidcBuildTimeConfig;
import io.quarkus.oidc.deployment.devservices.AbstractDevUIProcessor;
import io.quarkus.oidc.runtime.devui.OidcDevJsonRpcService;
import io.quarkus.oidc.runtime.devui.OidcDevUiRecorder;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HttpConfiguration;

public class KeycloakDevUIProcessor extends AbstractDevUIProcessor {

    OidcBuildTimeConfig oidcConfig;

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = IsDevelopment.class)
    @Consume(RuntimeConfigSetupCompleteBuildItem.class)
    void produceProviderComponent(Optional<KeycloakDevServicesConfigBuildItem> configProps,
            BuildProducer<KeycloakAdminPageBuildItem> keycloakAdminPageProducer,
            HttpConfiguration httpConfiguration,
            OidcDevUiRecorder recorder,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            BeanContainerBuildItem beanContainer,
            ConfigurationBuildItem configurationBuildItem,
            Capabilities capabilities) {
        final String keycloakAdminUrl = KeycloakDevServicesConfigBuildItem.getKeycloakUrl(configProps);
        if (keycloakAdminUrl != null) {
            String realmUrl = configProps.get().getConfig().get("quarkus.oidc.auth-server-url");
            @SuppressWarnings("unchecked")
            Map<String, String> users = (Map<String, String>) configProps.get().getProperties().get("oidc.users");

            @SuppressWarnings("unchecked")
            final List<String> keycloakRealms = (List<String>) configProps.get().getProperties().get("keycloak.realms");

            CardPageBuildItem cardPageBuildItem = createProviderWebComponent(
                    recorder,
                    capabilities,
                    "Keycloak",
                    configProps.get().getConfig().get("quarkus.oidc.application-type"),
                    oidcConfig.devui().grant().type().orElse(DevUiConfig.Grant.Type.CODE).getGrantType(),
                    realmUrl + "/protocol/openid-connect/auth",
                    realmUrl + "/protocol/openid-connect/token",
                    realmUrl + "/protocol/openid-connect/logout",
                    true,
                    beanContainer,
                    oidcConfig.devui().webClientTimeout(),
                    oidcConfig.devui().grantOptions(),
                    nonApplicationRootPathBuildItem,
                    configurationBuildItem,
                    keycloakAdminUrl,
                    users,
                    keycloakRealms,
                    configProps.get().isContainerRestarted(),
                    httpConfiguration);
            // use same card page so that both pages appear on the same card
            var keycloakAdminPageItem = new KeycloakAdminPageBuildItem(cardPageBuildItem);
            keycloakAdminPageProducer.produce(keycloakAdminPageItem);
        }
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    JsonRPCProvidersBuildItem produceOidcDevJsonRpcService() {
        return new JsonRPCProvidersBuildItem(OidcDevJsonRpcService.class);
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = IsDevelopment.class)
    void invokeEndpoint(BuildProducer<RouteBuildItem> routeProducer,
            OidcDevUiRecorder recorder,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {
        routeProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .nestedRoute("io.quarkus.quarkus-oidc", "readSessionCookie")
                .handler(recorder.readSessionCookieHandler())
                .build());
        routeProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .nestedRoute("io.quarkus.quarkus-oidc", "logout")
                .handler(recorder.logoutHandler())
                .build());
    }
}
