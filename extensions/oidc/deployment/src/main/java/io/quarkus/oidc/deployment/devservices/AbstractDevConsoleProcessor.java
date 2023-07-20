package io.quarkus.oidc.deployment.devservices;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.ConfigurationBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.devui.spi.page.PageBuilder;
import io.quarkus.oidc.runtime.OidcConfigPropertySupplier;
import io.quarkus.oidc.runtime.devui.OidcDevUiRecorder;
import io.quarkus.oidc.runtime.devui.OidcDevUiRpcSvcPropertiesBean;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;

public abstract class AbstractDevConsoleProcessor {
    private static final Logger LOG = Logger.getLogger(AbstractDevConsoleProcessor.class);
    protected static final String CONFIG_PREFIX = "quarkus.oidc.";
    protected static final String CLIENT_ID_CONFIG_KEY = CONFIG_PREFIX + "client-id";
    protected static final String CLIENT_SECRET_CONFIG_KEY = CONFIG_PREFIX + "credentials.secret";
    protected static final String AUTHORIZATION_PATH_CONFIG_KEY = CONFIG_PREFIX + "authorization-path";
    protected static final String TOKEN_PATH_CONFIG_KEY = CONFIG_PREFIX + "token-path";
    protected static final String END_SESSION_PATH_CONFIG_KEY = CONFIG_PREFIX + "end-session-path";
    protected static final String POST_LOGOUT_URI_PARAM_CONFIG_KEY = CONFIG_PREFIX + "logout.post-logout-uri-param";
    protected static final String SCOPES_KEY = CONFIG_PREFIX + "authentication.scopes";

    protected void produceDevConsoleTemplateItems(Capabilities capabilities,
            BuildProducer<DevConsoleTemplateInfoBuildItem> devConsoleTemplate,
            BuildProducer<DevConsoleRuntimeTemplateInfoBuildItem> devConsoleRuntimeInfo,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            String oidcProviderName,
            String oidcApplicationType,
            String oidcGrantType,
            String authorizationUrl,
            String tokenUrl,
            String logoutUrl,
            boolean introspectionIsAvailable) {
        if (oidcProviderName != null) {
            devConsoleTemplate.produce(new DevConsoleTemplateInfoBuildItem("oidcProviderName", oidcProviderName));
        }
        devConsoleTemplate.produce(new DevConsoleTemplateInfoBuildItem("oidcApplicationType", oidcApplicationType));
        devConsoleTemplate.produce(new DevConsoleTemplateInfoBuildItem("oidcGrantType", oidcGrantType));

        if (capabilities.isPresent(Capability.SMALLRYE_OPENAPI)) {
            devConsoleTemplate.produce(new DevConsoleTemplateInfoBuildItem("swaggerIsAvailable", true));
        }
        if (capabilities.isPresent(Capability.SMALLRYE_GRAPHQL)) {
            devConsoleTemplate.produce(new DevConsoleTemplateInfoBuildItem("graphqlIsAvailable", true));
        }
        devConsoleTemplate.produce(new DevConsoleTemplateInfoBuildItem("introspectionIsAvailable", introspectionIsAvailable));

        devConsoleRuntimeInfo.produce(
                new DevConsoleRuntimeTemplateInfoBuildItem("clientId",
                        new OidcConfigPropertySupplier(CLIENT_ID_CONFIG_KEY), this.getClass(), curateOutcomeBuildItem));
        devConsoleRuntimeInfo.produce(
                new DevConsoleRuntimeTemplateInfoBuildItem("clientSecret",
                        new OidcConfigPropertySupplier(CLIENT_SECRET_CONFIG_KEY, ""), this.getClass(), curateOutcomeBuildItem));
        devConsoleRuntimeInfo.produce(
                new DevConsoleRuntimeTemplateInfoBuildItem("authorizationUrl",
                        new OidcConfigPropertySupplier(AUTHORIZATION_PATH_CONFIG_KEY, authorizationUrl, true), this.getClass(),
                        curateOutcomeBuildItem));
        devConsoleRuntimeInfo.produce(
                new DevConsoleRuntimeTemplateInfoBuildItem("tokenUrl",
                        new OidcConfigPropertySupplier(TOKEN_PATH_CONFIG_KEY, tokenUrl, true), this.getClass(),
                        curateOutcomeBuildItem));
        devConsoleRuntimeInfo.produce(
                new DevConsoleRuntimeTemplateInfoBuildItem("logoutUrl",
                        new OidcConfigPropertySupplier(END_SESSION_PATH_CONFIG_KEY, logoutUrl, true), this.getClass(),
                        curateOutcomeBuildItem));
        devConsoleRuntimeInfo.produce(
                new DevConsoleRuntimeTemplateInfoBuildItem("postLogoutUriParam",
                        new OidcConfigPropertySupplier(POST_LOGOUT_URI_PARAM_CONFIG_KEY), this.getClass(),
                        curateOutcomeBuildItem));
        devConsoleRuntimeInfo.produce(
                new DevConsoleRuntimeTemplateInfoBuildItem("scopes",
                        new OidcConfigPropertySupplier(SCOPES_KEY), this.getClass(), curateOutcomeBuildItem));

    }

    protected void produceDevConsoleRouteItems(BuildProducer<DevConsoleRouteBuildItem> devConsoleRoute,
            DevConsolePostHandler testServiceWithToken,
            DevConsolePostHandler exchangeCodeForTokens,
            DevConsolePostHandler passwordClientCredHandler) {
        devConsoleRoute.produce(new DevConsoleRouteBuildItem("testServiceWithToken", "POST", testServiceWithToken));
        devConsoleRoute.produce(new DevConsoleRouteBuildItem("exchangeCodeForTokens", "POST", exchangeCodeForTokens));
        devConsoleRoute.produce(new DevConsoleRouteBuildItem("testService", "POST", passwordClientCredHandler));
    }

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
            boolean alwaysLogoutUserInDevUiOnReload, PageBuilder<?> customPage) {
        final CardPageBuildItem cardPage = new CardPageBuildItem();

        if (customPage == null) {
            // prepare provider component
            cardPage.addPage(Page
                    .webComponentPageBuilder()
                    .icon("font-awesome-solid:boxes-stacked")
                    .title(oidcProviderName == null ? "OpenId Connect Dev Console" : oidcProviderName + " provider")
                    .componentLink("qwc-oidc-provider.js"));
        } else {
            // other extension provided customized version of OIDC provider page
            //            LOG.infof("Default OIDC provider page will be replaced with the '%s' page", customPage.getTitle()); TODO
            cardPage.addPage(customPage);
            // even for custom provider page we are going to provide build time and runtime data in case they are needed
        }

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
