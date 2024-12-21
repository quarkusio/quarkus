package io.quarkus.oidc.runtime.devui;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class OidcDevUiRecorder {

    private final RuntimeValue<OidcConfig> oidcConfigRuntimeValue;

    public OidcDevUiRecorder(RuntimeValue<OidcConfig> oidcConfigRuntimeValue) {
        this.oidcConfigRuntimeValue = oidcConfigRuntimeValue;
    }

    public void createJsonRPCService(BeanContainer beanContainer,
            RuntimeValue<OidcDevUiRpcSvcPropertiesBean> oidcDevUiRpcSvcPropertiesBean, HttpConfiguration httpConfiguration) {
        OidcDevJsonRpcService jsonRpcService = beanContainer.beanInstance(OidcDevJsonRpcService.class);
        jsonRpcService.hydrate(oidcDevUiRpcSvcPropertiesBean.getValue(), httpConfiguration);
    }

    public RuntimeValue<OidcDevUiRpcSvcPropertiesBean> getRpcServiceProperties(String authorizationUrl, String tokenUrl,
            String logoutUrl, Duration webClientTimeout, Map<String, Map<String, String>> grantOptions,
            Map<String, String> oidcUsers, String oidcProviderName, String oidcApplicationType, String oidcGrantType,
            boolean introspectionIsAvailable, String keycloakAdminUrl, List<String> keycloakRealms, boolean swaggerIsAvailable,
            boolean graphqlIsAvailable, String swaggerUiPath, String graphqlUiPath, boolean alwaysLogoutUserInDevUiOnReload) {

        return new RuntimeValue<OidcDevUiRpcSvcPropertiesBean>(
                new OidcDevUiRpcSvcPropertiesBean(authorizationUrl, tokenUrl, logoutUrl,
                        webClientTimeout, grantOptions, oidcUsers, oidcProviderName, oidcApplicationType, oidcGrantType,
                        introspectionIsAvailable, keycloakAdminUrl, keycloakRealms, swaggerIsAvailable,
                        graphqlIsAvailable, swaggerUiPath, graphqlUiPath, alwaysLogoutUserInDevUiOnReload));
    }

    public Handler<RoutingContext> readSessionCookieHandler() {
        return new OidcDevSessionCookieReaderHandler(oidcConfigRuntimeValue.getValue());
    }

    public Handler<RoutingContext> logoutHandler() {
        return new OidcDevSessionLogoutHandler();
    }
}
