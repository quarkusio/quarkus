package io.quarkus.oidc.deployment.devservices.keycloak;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ConfigurationBuildItem;
import io.quarkus.deployment.builditem.RuntimeConfigSetupCompleteBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.oidc.deployment.OidcBuildTimeConfig;
import io.quarkus.oidc.deployment.devservices.AbstractDevConsoleProcessor;
import io.quarkus.oidc.deployment.devservices.CustomOidcDevUiProviderPageBuildItem;
import io.quarkus.oidc.deployment.devservices.OidcAuthorizationCodePostHandler;
import io.quarkus.oidc.deployment.devservices.OidcPasswordClientCredHandler;
import io.quarkus.oidc.deployment.devservices.OidcTestServiceHandler;
import io.quarkus.oidc.runtime.devui.OidcDevJsonRpcService;
import io.quarkus.oidc.runtime.devui.OidcDevUiRecorder;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;

public class KeycloakDevConsoleProcessor extends AbstractDevConsoleProcessor {

    KeycloakBuildTimeConfig keycloakConfig;
    OidcBuildTimeConfig oidcConfig;

    @BuildStep(onlyIf = IsDevelopment.class)
    @Consume(RuntimeConfigSetupCompleteBuildItem.class)
    public void setConfigProperties(BuildProducer<DevConsoleTemplateInfoBuildItem> devConsoleInfo,
            BuildProducer<DevConsoleRuntimeTemplateInfoBuildItem> devConsoleRuntimeInfo,
            Optional<KeycloakDevServicesConfigBuildItem> configProps,
            Capabilities capabilities, CurateOutcomeBuildItem curateOutcomeBuildItem) {
        if (configProps.isPresent() && configProps.get().getConfig().containsKey("keycloak.url")) {
            devConsoleInfo.produce(
                    new DevConsoleTemplateInfoBuildItem("keycloakAdminUrl", configProps.get().getConfig().get("keycloak.url")));
            devConsoleInfo.produce(
                    new DevConsoleTemplateInfoBuildItem("keycloakUsers",
                            configProps.get().getProperties().get("oidc.users")));
            devConsoleInfo.produce(
                    new DevConsoleTemplateInfoBuildItem("keycloakRealms",
                            configProps.get().getProperties().get("keycloak.realms")));

            String realmUrl = configProps.get().getConfig().get("quarkus.oidc.auth-server-url");

            produceDevConsoleTemplateItems(capabilities,
                    devConsoleInfo,
                    devConsoleRuntimeInfo,
                    curateOutcomeBuildItem,
                    "Keycloak",
                    (String) configProps.get().getConfig().get("quarkus.oidc.application-type"),
                    oidcConfig.devui.grant.type.isPresent() ? oidcConfig.devui.grant.type.get().getGrantType()
                            : keycloakConfig.devservices.grant.type.getGrantType(),
                    realmUrl + "/protocol/openid-connect/auth",
                    realmUrl + "/protocol/openid-connect/token",
                    realmUrl + "/protocol/openid-connect/logout",
                    true);
        }
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = IsDevelopment.class)
    @Consume(RuntimeConfigSetupCompleteBuildItem.class)
    void produceProviderComponent(Optional<KeycloakDevServicesConfigBuildItem> configProps,
            BuildProducer<CardPageBuildItem> cardPageProducer,
            OidcDevUiRecorder recorder,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer,
            ConfigurationBuildItem configurationBuildItem,
            Capabilities capabilities,
            Optional<CustomOidcDevUiProviderPageBuildItem> customProviderPage) {
        if (configProps.isPresent() && configProps.get().getConfig().containsKey("keycloak.url")) {
            String realmUrl = configProps.get().getConfig().get("quarkus.oidc.auth-server-url");
            @SuppressWarnings("unchecked")
            Map<String, String> users = (Map<String, String>) configProps.get().getProperties().get("oidc.users");

            String keycloakAdminUrl = configProps.get().getConfig().get("keycloak.url");

            @SuppressWarnings("unchecked")
            final List<String> keycloakRealms = (List<String>) configProps.get().getProperties().get("keycloak.realms");

            CardPageBuildItem cardPageBuildItem = createProviderWebComponent(
                    recorder,
                    capabilities,
                    "Keycloak",
                    configProps.get().getConfig().get("quarkus.oidc.application-type"),
                    oidcConfig.devui.grant.type.isPresent() ? oidcConfig.devui.grant.type.get().getGrantType()
                            : keycloakConfig.devservices.grant.type.getGrantType(),
                    realmUrl + "/protocol/openid-connect/auth",
                    realmUrl + "/protocol/openid-connect/token",
                    realmUrl + "/protocol/openid-connect/logout",
                    true,
                    syntheticBeanBuildItemBuildProducer,
                    oidcConfig.devui.webClientTimeout,
                    oidcConfig.devui.grantOptions,
                    nonApplicationRootPathBuildItem,
                    configurationBuildItem,
                    keycloakAdminUrl,
                    users,
                    keycloakRealms,
                    configProps.get().isContainerRestarted(),
                    customProviderPage.map(CustomOidcDevUiProviderPageBuildItem::getOidcProviderPage).orElse(null));

            // Also add Admin page
            cardPageBuildItem.addPage(Page.externalPageBuilder("Keycloak Admin")
                    .icon("font-awesome-solid:key")
                    .doNotEmbed(true)
                    .url(keycloakAdminUrl));
            cardPageProducer.produce(cardPageBuildItem);
        }
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    JsonRPCProvidersBuildItem produceOidcDevJsonRpcService() {
        return new JsonRPCProvidersBuildItem(OidcDevJsonRpcService.class);
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    void invokeEndpoint(BuildProducer<DevConsoleRouteBuildItem> devConsoleRoute,
            Optional<KeycloakDevServicesConfigBuildItem> configProps) {
        if (configProps.isPresent() && configProps.get().getConfig().containsKey("keycloak.url")) {
            @SuppressWarnings("unchecked")
            Map<String, String> users = (Map<String, String>) configProps.get().getProperties().get("oidc.users");
            produceDevConsoleRouteItems(devConsoleRoute,
                    new OidcTestServiceHandler(KeycloakDevServicesProcessor.vertxInstance, oidcConfig.devui.webClientTimeout),
                    new OidcAuthorizationCodePostHandler(KeycloakDevServicesProcessor.vertxInstance,
                            oidcConfig.devui.webClientTimeout,
                            oidcConfig.devui.grantOptions),
                    new OidcPasswordClientCredHandler(KeycloakDevServicesProcessor.vertxInstance,
                            oidcConfig.devui.webClientTimeout, users,
                            oidcConfig.devui.grantOptions));
        }
    }
}
