package io.quarkus.oidc.deployment.devservices;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import jakarta.inject.Singleton;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.ConfigurationBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.oidc.runtime.devui.OidcDevUiRecorder;
import io.quarkus.oidc.runtime.devui.OidcDevUiRpcSvcPropertiesBean;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;

public abstract class AbstractDevUIProcessor {
    protected static final String CONFIG_PREFIX = "quarkus.oidc.";
    protected static final String CLIENT_ID_CONFIG_KEY = CONFIG_PREFIX + "client-id";

    protected static CardPageBuildItem createProviderWebComponent(OidcDevUiRecorder recorder,
            Capabilities capabilities,
            String oidcProviderName,
            String oidcApplicationType,
            String oidcGrantType,
            String authorizationUrl,
            String tokenUrl,
            String logoutUrl,
            boolean introspectionIsAvailable,
            BuildProducer<SyntheticBeanBuildItem> beanProducer,
            Duration webClientTimeout,
            Map<String, Map<String, String>> grantOptions, NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            ConfigurationBuildItem configurationBuildItem,
            String keycloakAdminUrl,
            Map<String, String> keycloakUsers,
            List<String> keycloakRealms,
            boolean alwaysLogoutUserInDevUiOnReload) {
        final CardPageBuildItem cardPage = new CardPageBuildItem();

        // prepare provider component
        cardPage.addPage(Page
                .webComponentPageBuilder()
                .icon("font-awesome-solid:boxes-stacked")
                .title(oidcProviderName == null ? "OpenId Connect Dev Console" : oidcProviderName + " provider")
                .componentLink("qwc-oidc-provider.js"));

        // prepare data for provider component
        final boolean swaggerIsAvailable = capabilities.isPresent(Capability.SMALLRYE_OPENAPI);
        final boolean graphqlIsAvailable = capabilities.isPresent(Capability.SMALLRYE_GRAPHQL);

        final String swaggerUiPath;
        if (swaggerIsAvailable) {
            swaggerUiPath = nonApplicationRootPathBuildItem.resolvePath(
                    getProperty(configurationBuildItem, "quarkus.swagger-ui.path"));
        } else {
            swaggerUiPath = null;
        }

        final String graphqlUiPath;
        if (graphqlIsAvailable) {
            graphqlUiPath = nonApplicationRootPathBuildItem.resolvePath(
                    getProperty(configurationBuildItem, "quarkus.smallrye-graphql.ui.root-path"));
        } else {
            graphqlUiPath = null;
        }

        cardPage.addBuildTimeData("devRoot", nonApplicationRootPathBuildItem.getNonApplicationRootPath());

        // pass down properties used by RPC service
        beanProducer.produce(
                SyntheticBeanBuildItem.configure(OidcDevUiRpcSvcPropertiesBean.class).unremovable()
                        .supplier(recorder.prepareRpcServiceProperties(authorizationUrl, tokenUrl, logoutUrl,
                                webClientTimeout, grantOptions, keycloakUsers, oidcProviderName, oidcApplicationType,
                                oidcGrantType, introspectionIsAvailable, keycloakAdminUrl, keycloakRealms,
                                swaggerIsAvailable, graphqlIsAvailable, swaggerUiPath, graphqlUiPath,
                                alwaysLogoutUserInDevUiOnReload))
                        .scope(Singleton.class)
                        .setRuntimeInit()
                        .done());

        return cardPage;
    }

    private static String getProperty(ConfigurationBuildItem configurationBuildItem,
            String propertyKey) {
        // strictly speaking we know 'quarkus.swagger-ui.path' is build time property
        // and 'quarkus.smallrye-graphql.ui.root-path' is build time with runtime fixed,
        // but I wanted to make this bit more robust till we have DEV UI tests
        // that will fail when this get changed in the future, then we can optimize this

        String propertyValue = configurationBuildItem
                .getReadResult()
                .getAllBuildTimeValues()
                .get(propertyKey);

        if (propertyValue == null) {
            propertyValue = configurationBuildItem
                    .getReadResult()
                    .getBuildTimeRunTimeValues()
                    .get(propertyKey);
        } else {
            return propertyValue;
        }

        if (propertyValue == null) {
            propertyValue = configurationBuildItem
                    .getReadResult()
                    .getRunTimeDefaultValues()
                    .get(propertyKey);
        }

        return propertyValue;
    }
}
