package io.quarkus.oidc.deployment.devservices;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;
import io.quarkus.oidc.runtime.OidcConfigPropertySupplier;

public abstract class AbstractDevConsoleProcessor {
    protected static final String CONFIG_PREFIX = "quarkus.oidc.";
    protected static final String CLIENT_ID_CONFIG_KEY = CONFIG_PREFIX + "client-id";
    protected static final String CLIENT_SECRET_CONFIG_KEY = CONFIG_PREFIX + "credentials.secret";
    protected static final String AUTHORIZATION_PATH_CONFIG_KEY = CONFIG_PREFIX + "authorization-path";
    protected static final String TOKEN_PATH_CONFIG_KEY = CONFIG_PREFIX + "token-path";
    protected static final String END_SESSION_PATH_CONFIG_KEY = CONFIG_PREFIX + "end-session-path";
    protected static final String POST_LOGOUT_URI_PARAM_CONFIG_KEY = CONFIG_PREFIX + "logout.post-logout-uri-param";

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

    }

    protected void produceDevConsoleRouteItems(BuildProducer<DevConsoleRouteBuildItem> devConsoleRoute,
            DevConsolePostHandler testServiceWithToken,
            DevConsolePostHandler exchangeCodeForTokens,
            DevConsolePostHandler passwordClientCredHandler) {
        devConsoleRoute.produce(new DevConsoleRouteBuildItem("testServiceWithToken", "POST", testServiceWithToken));
        devConsoleRoute.produce(new DevConsoleRouteBuildItem("exchangeCodeForTokens", "POST", exchangeCodeForTokens));
        devConsoleRoute.produce(new DevConsoleRouteBuildItem("testService", "POST", passwordClientCredHandler));
    }
}
