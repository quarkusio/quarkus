package io.quarkus.oidc.deployment.devservices;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.oidc.runtime.OidcTenantConfig;
import io.quarkus.oidc.runtime.dev.ui.OidcDevJsonRpcService;
import io.quarkus.oidc.runtime.dev.ui.OidcDevSessionCookieReaderHandler;
import io.quarkus.oidc.runtime.dev.ui.OidcDevSessionLogoutHandler;
import io.quarkus.oidc.runtime.dev.ui.OidcDevUiBuildTimeProperties;
import io.quarkus.oidc.runtime.dev.ui.OidcDevUiBuildTimeProperties.DevServiceType;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.smallrye.config.Config;
import io.vertx.core.Vertx;

public abstract class AbstractDevUIProcessor {
    protected static final String CONFIG_PREFIX = "quarkus.oidc.";
    protected static final String CLIENT_ID_CONFIG_KEY = CONFIG_PREFIX + "client-id";
    private static final String APP_TYPE_CONFIG_KEY = CONFIG_PREFIX + "application-type";

    protected static CardPageBuildItem createProviderWebComponent(ServiceRegistrar serviceRegistrar,
            Capabilities capabilities,
            String oidcProviderName,
            String oidcGrantType,
            boolean introspectionIsAvailable,
            Duration webClientTimeout,
            Map<String, Map<String, String>> grantOptions,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            String devServiceConfigHashCode,
            boolean discoverMetadata,
            String authServerUrl,
            String buildTimeKeycloakAdminUrl,
            String buildTimeOidcApplicationType, DevServiceType devServiceType) {
        final CardPageBuildItem cardPage = new CardPageBuildItem();

        cardPage.setLogo("oidc_logo.png", "oidc_logo.png");

        // prepare provider component
        cardPage.addPage(Page
                .webComponentPageBuilder()
                .icon("font-awesome-solid:boxes-stacked")
                .title(oidcProviderName == null ? "OpenId Connect Dev Console" : oidcProviderName + " provider")
                .componentLink("qwc-oidc-provider.js"));

        // prepare data for provider component
        final boolean swaggerIsAvailable = capabilities.isPresent(Capability.SMALLRYE_OPENAPI);
        final boolean graphqlIsAvailable = capabilities.isPresent(Capability.SMALLRYE_GRAPHQL);
        final var config = Config.get();

        final String swaggerUiPath;
        if (swaggerIsAvailable) {
            swaggerUiPath = nonApplicationRootPathBuildItem.resolvePath(
                    config.getValue("quarkus.swagger-ui.path", String.class));
        } else {
            swaggerUiPath = null;
        }

        final String graphqlUiPath;
        if (graphqlIsAvailable) {
            graphqlUiPath = nonApplicationRootPathBuildItem.resolvePath(
                    config.getValue("quarkus.smallrye-graphql.ui.root-path", String.class));
        } else {
            graphqlUiPath = null;
        }

        final String devUiLogoutPath = nonApplicationRootPathBuildItem.resolvePath("quarkus-oidc/logout");
        final String devUiReadSessionCookiePath = nonApplicationRootPathBuildItem
                .resolvePath("quarkus-oidc/readSessionCookie");

        cardPage.addBuildTimeData("devRoot", nonApplicationRootPathBuildItem.getNonApplicationRootPath());

        // the service lambda can only capture unmodifiable maps, and the config mapping maps are HashMaps
        Map<String, Map<String, String>> unmodifiableGrantOptions = grantOptions.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> Map.copyOf(e.getValue())));
        var buildTimeProperties = new OidcDevUiBuildTimeProperties(webClientTimeout, unmodifiableGrantOptions,
                oidcProviderName, oidcGrantType, introspectionIsAvailable,
                swaggerIsAvailable, graphqlIsAvailable, swaggerUiPath, graphqlUiPath, devServiceConfigHashCode,
                discoverMetadata, devUiLogoutPath, devUiReadSessionCookiePath, authServerUrl, buildTimeKeycloakAdminUrl,
                buildTimeOidcApplicationType, devServiceType);

        serviceRegistrar
                .forService("io.quarkus.oidc.dev-ui.prepare-json-rpc-service")
                .require(BeanContainer.class)
                // Vert.x comes from its service rather than an injection point, so that this service does not wait for
                // the runtime-init synthetic beans.
                .require(Vertx.class)
                .onStart((ctx, beanContainer, vertx) -> beanContainer.beanInstance(OidcDevJsonRpcService.class)
                        .hydrate(buildTimeProperties, vertx));

        return cardPage;
    }

    protected static String getApplicationType(OidcTenantConfig providerConfig) {
        Optional<io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType> appType = Config.get()
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

    protected static void registerOidcWebAppRoutes(BuildProducer<RouteBuildItem> routeProducer,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem) {
        routeProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .nestedRoute("quarkus-oidc", "readSessionCookie")
                .handler(new OidcDevSessionCookieReaderHandler())
                .build());
        routeProducer.produce(nonApplicationRootPathBuildItem.routeBuilder()
                .nestedRoute("quarkus-oidc", "logout")
                .handler(new OidcDevSessionLogoutHandler())
                .build());
    }
}
