package io.quarkus.oidc.deployment.devservices.keycloak;

import java.util.Optional;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.RuntimeConfigSetupCompleteBuildItem;
import io.quarkus.devservices.keycloak.KeycloakAdminPageBuildItem;
import io.quarkus.devservices.keycloak.KeycloakDevServicesPreparedBuildItem;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.oidc.deployment.DevUiConfig;
import io.quarkus.oidc.deployment.OidcBuildTimeConfig;
import io.quarkus.oidc.deployment.devservices.AbstractDevUIProcessor;
import io.quarkus.oidc.runtime.dev.ui.OidcDevJsonRpcService;
import io.quarkus.oidc.runtime.dev.ui.OidcDevLoginObserver;
import io.quarkus.oidc.runtime.dev.ui.OidcDevUiRecorder;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;

public class KeycloakDevUIProcessor extends AbstractDevUIProcessor {

    OidcBuildTimeConfig oidcConfig;

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = IsLocalDevelopment.class)
    @Consume(RuntimeConfigSetupCompleteBuildItem.class)
    void produceProviderComponent(BuildProducer<KeycloakAdminPageBuildItem> keycloakAdminPageProducer,
            OidcDevUiRecorder recorder,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            BeanContainerBuildItem beanContainer,
            Capabilities capabilities,
            Optional<KeycloakDevServicesPreparedBuildItem> keycloakDevServicesPreparedBuildItem) {
        if (keycloakDevServicesPreparedBuildItem.isPresent()) {
            CardPageBuildItem cardPageBuildItem = createProviderWebComponent(
                    recorder,
                    capabilities,
                    "Keycloak",
                    oidcConfig.devui().grant().type().orElse(DevUiConfig.Grant.Type.CODE).getGrantType(),
                    true,
                    beanContainer,
                    oidcConfig.devui().webClientTimeout(),
                    oidcConfig.devui().grantOptions(),
                    nonApplicationRootPathBuildItem,
                    keycloakDevServicesPreparedBuildItem.get().getDevServiceConfigHashCode(),
                    false, null, null, null);

            cardPageBuildItem.setLogo("keycloak_logo.svg", "keycloak_logo.svg");

            // use same card page so that both pages appear on the same card
            var keycloakAdminPageItem = new KeycloakAdminPageBuildItem(cardPageBuildItem);
            keycloakAdminPageProducer.produce(keycloakAdminPageItem);
        }
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    JsonRPCProvidersBuildItem produceOidcDevJsonRpcService() {
        return new JsonRPCProvidersBuildItem(OidcDevJsonRpcService.class);
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    AdditionalBeanBuildItem registerOidcDevLoginObserver() {
        // TODO: this is called even when Keycloak DEV UI is disabled and OIDC DEV UI is enabled
        //   we should fine a mechanism to switch where the endpoints are registered or have shared build steps
        return AdditionalBeanBuildItem.unremovableOf(OidcDevLoginObserver.class);
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = IsLocalDevelopment.class)
    void invokeEndpoint(BuildProducer<RouteBuildItem> routeProducer, OidcDevUiRecorder recorder,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {
        // TODO: this is called even when Keycloak DEV UI is disabled and OIDC DEV UI is enabled
        //   we should fine a mechanism to switch where the endpoints are registered or have shared build steps
        registerOidcWebAppRoutes(routeProducer, recorder, nonApplicationRootPathBuildItem);
    }

}
