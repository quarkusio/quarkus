package io.quarkus.oidc.runtime.devui;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class OidcDevUiRecorder {
    public Supplier<OidcDevUiRpcSvcPropertiesBean> prepareRpcServiceProperties(String authorizationUrl, String tokenUrl,
            String logoutUrl, Duration webClientTimeout, Map<String, Map<String, String>> grantOptions,
            Map<String, String> oidcUsers, String oidcProviderName, String oidcApplicationType, String oidcGrantType,
            boolean introspectionIsAvailable, String keycloakAdminUrl, List<String> keycloakRealms, boolean swaggerIsAvailable,
            boolean graphqlIsAvailable, String swaggerUiPath, String graphqlUiPath, boolean alwaysLogoutUserInDevUiOnReload) {
        return new Supplier<OidcDevUiRpcSvcPropertiesBean>() {
            @Override
            public OidcDevUiRpcSvcPropertiesBean get() {
                return new OidcDevUiRpcSvcPropertiesBean(authorizationUrl, tokenUrl, logoutUrl,
                        webClientTimeout, grantOptions, oidcUsers, oidcProviderName, oidcApplicationType, oidcGrantType,
                        introspectionIsAvailable, keycloakAdminUrl, keycloakRealms, swaggerIsAvailable,
                        graphqlIsAvailable, swaggerUiPath, graphqlUiPath, alwaysLogoutUserInDevUiOnReload);
            }
        };
    }
}
