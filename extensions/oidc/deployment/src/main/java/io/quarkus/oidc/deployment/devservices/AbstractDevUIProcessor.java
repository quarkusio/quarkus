package io.quarkus.oidc.deployment.devservices;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.ConfigurationBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.runtime.devui.OidcDevUiRecorder;
import io.quarkus.oidc.runtime.devui.OidcDevUiRpcSvcPropertiesBean;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.smallrye.config.ConfigValue;

public abstract class AbstractDevUIProcessor {
    protected static final String CONFIG_PREFIX = "quarkus.oidc.";
    protected static final String CLIENT_ID_CONFIG_KEY = CONFIG_PREFIX + "client-id";
    private static final String APP_TYPE_CONFIG_KEY = CONFIG_PREFIX + "application-type";

    protected static CardPageBuildItem createProviderWebComponent(OidcDevUiRecorder recorder,
            Capabilities capabilities,
            String oidcProviderName,
            String oidcApplicationType,
            String oidcGrantType,
            String authorizationUrl,
            String tokenUrl,
            String logoutUrl,
            boolean introspectionIsAvailable,
            BeanContainerBuildItem beanContainer,
            Duration webClientTimeout,
            Map<String, Map<String, String>> grantOptions,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            ConfigurationBuildItem configurationBuildItem,
            String keycloakAdminUrl,
            Map<String, String> keycloakUsers,
            List<String> keycloakRealms,
            boolean alwaysLogoutUserInDevUiOnReload,
            HttpConfiguration httpConfiguration) {
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

        RuntimeValue<OidcDevUiRpcSvcPropertiesBean> runtimeProperties = recorder.getRpcServiceProperties(
                authorizationUrl, tokenUrl, logoutUrl, webClientTimeout, grantOptions,
                keycloakUsers, oidcProviderName, oidcApplicationType, oidcGrantType,
                introspectionIsAvailable, keycloakAdminUrl, keycloakRealms, swaggerIsAvailable,
                graphqlIsAvailable, swaggerUiPath, graphqlUiPath, alwaysLogoutUserInDevUiOnReload);

        recorder.createJsonRPCService(beanContainer.getValue(), runtimeProperties, httpConfiguration);

        return cardPage;
    }

    private static String getProperty(ConfigurationBuildItem configurationBuildItem,
            String propertyKey) {
        // strictly speaking we know 'quarkus.swagger-ui.path' is build time property
        // and 'quarkus.smallrye-graphql.ui.root-path' is build time with runtime fixed,
        // but I wanted to make this bit more robust till we have DEV UI tests
        // that will fail when this get changed in the future, then we can optimize this

        ConfigValue configValue = configurationBuildItem
                .getReadResult()
                .getAllBuildTimeValues()
                .get(propertyKey);

        if (configValue == null || configValue.getValue() == null) {
            configValue = configurationBuildItem
                    .getReadResult()
                    .getBuildTimeRunTimeValues()
                    .get(propertyKey);
        } else {
            return configValue.getValue();
        }

        if (configValue == null || configValue.getValue() == null) {
            configValue = configurationBuildItem
                    .getReadResult()
                    .getRunTimeDefaultValues()
                    .get(propertyKey);
        }

        return configValue != null ? configValue.getValue() : null;
    }

    protected static String getApplicationType() {
        return getApplicationType(null);
    }

    protected static String getApplicationType(OidcTenantConfig providerConfig) {
        Optional<io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType> appType = ConfigProvider.getConfig()
                .getOptionalValue(APP_TYPE_CONFIG_KEY,
                        io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType.class);
        if (appType.isEmpty() && providerConfig != null) {
            appType = providerConfig.applicationType();
        }
        return appType
                // constant is "WEB_APP" while documented value is "web-app" and we expect users to use "web-app"
                // if this get changed, we need to update qwc-oidc-provider.js as well
                .map(at -> io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType.WEB_APP == at ? "web-app"
                        : at.name().toLowerCase())
                .orElse(OidcTenantConfig.ApplicationType.SERVICE.name().toLowerCase());
    }

    protected static void registerOidcWebAppRoutes(BuildProducer<RouteBuildItem> routeProducer, OidcDevUiRecorder recorder,
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
