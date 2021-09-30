package io.quarkus.oidc.deployment.devservices;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.devconsole.runtime.spi.DevConsolePostHandler;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;

public abstract class AbstractDevConsoleProcessor {
    protected void produceDevConsoleTemplateItems(Capabilities capabilities,
            BuildProducer<DevConsoleTemplateInfoBuildItem> devConsoleTemplate,
            String oidcProviderName,
            String oidcApplicationType,
            String oidcGrantType,
            String clientId,
            String clientSecret,
            String authorizationUrl,
            String tokenUrl,
            String logoutUrl,
            boolean introspectionIsAvailable) {
        if (oidcProviderName != null) {
            devConsoleTemplate.produce(new DevConsoleTemplateInfoBuildItem("oidcProviderName", oidcProviderName));
        }
        devConsoleTemplate.produce(new DevConsoleTemplateInfoBuildItem("oidcApplicationType", oidcApplicationType));
        devConsoleTemplate.produce(new DevConsoleTemplateInfoBuildItem("oidcGrantType", oidcGrantType));

        devConsoleTemplate.produce(new DevConsoleTemplateInfoBuildItem("clientId", clientId));
        devConsoleTemplate.produce(new DevConsoleTemplateInfoBuildItem("clientSecret", clientSecret));

        devConsoleTemplate.produce(new DevConsoleTemplateInfoBuildItem("authorizationUrl", authorizationUrl));
        devConsoleTemplate.produce(new DevConsoleTemplateInfoBuildItem("tokenUrl", tokenUrl));
        if (logoutUrl != null) {
            devConsoleTemplate.produce(new DevConsoleTemplateInfoBuildItem("logoutUrl", logoutUrl));
        }
        if (capabilities.isPresent(Capability.SMALLRYE_OPENAPI)) {
            devConsoleTemplate.produce(new DevConsoleTemplateInfoBuildItem("swaggerIsAvailable", true));
        }
        if (capabilities.isPresent(Capability.SMALLRYE_GRAPHQL)) {
            devConsoleTemplate.produce(new DevConsoleTemplateInfoBuildItem("graphqlIsAvailable", true));
        }
        devConsoleTemplate.produce(new DevConsoleTemplateInfoBuildItem("introspectionIsAvailable", introspectionIsAvailable));
    }

    protected void produceDevConsoleRouteItems(BuildProducer<DevConsoleRouteBuildItem> devConsoleRoute,
            DevConsolePostHandler testServiceWithToken, DevConsolePostHandler exchangeCodeForTokens) {
        devConsoleRoute.produce(new DevConsoleRouteBuildItem("testServiceWithToken", "POST", testServiceWithToken));
        devConsoleRoute.produce(new DevConsoleRouteBuildItem("exchangeCodeForTokens", "POST", exchangeCodeForTokens));

    }
}
