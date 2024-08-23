package io.quarkus.oidc.runtime.devui;

import static io.quarkus.oidc.runtime.devui.OidcDevServicesUtils.getTokens;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.quarkus.arc.Arc;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

public class OidcDevJsonRpcService {

    private final String authorizationUrl;
    private final String tokenUrl;
    private final String logoutUrl;
    private final Duration timeout;
    private final Map<String, String> codeGrantOptions;
    private final Map<String, String> passwordGrantOptions;
    private final Map<String, String> clientCredGrantOptions;
    private final Map<String, String> oidcUserToPassword;
    private final int httpPort;
    private final SmallRyeConfig config;
    private final Vertx vertx;
    private final String oidcProviderName;
    private final String oidcApplicationType;
    private final String oidcGrantType;
    private final boolean introspectionIsAvailable;
    private final String keycloakAdminUrl;
    private final List<String> keycloakRealms;
    private final boolean swaggerIsAvailable;
    private final boolean graphqlIsAvailable;
    private final String swaggerUiPath;
    private final String graphqlUiPath;
    private final boolean alwaysLogoutUserInDevUiOnReload;
    private final String propertiesStateId;

    public OidcDevJsonRpcService(HttpConfiguration httpConfiguration, SmallRyeConfig config, Vertx vertx) {

        // we need to inject properties bean lazily as 'OidcDevJsonRpcService' is also produced when OIDC DEV UI is not
        // we must always produce it when in DEV mode because we can't check for 'KeycloakDevServicesConfigBuildItem'
        // due to circular reference: JSON RPC provider is additional bean and 'LoggingSetupBuildItem' used by
        // 'KeycloakDevServicesProcessor' is created with combined index
        final var propsInstanceHandle = Arc.container().instance(OidcDevUiRpcSvcPropertiesBean.class);
        final OidcDevUiRpcSvcPropertiesBean props;
        if (propsInstanceHandle.isAvailable()) {
            props = propsInstanceHandle.get();
        } else {
            // OIDC Dev UI is disabled, but this RPC service still gets initialized by Quarkus DEV UI
            props = new OidcDevUiRpcSvcPropertiesBean(null, null, null, null, Map.of(), Map.of(), null, null, null, false, null,
                    List.of(), false, false, null, null, false);
        }

        this.httpPort = httpConfiguration.port;
        this.config = config;
        this.vertx = vertx;
        this.authorizationUrl = props.getAuthorizationUrl();
        this.tokenUrl = props.getTokenUrl();
        this.logoutUrl = props.getLogoutUrl();
        this.timeout = props.getWebClientTimeout();
        this.codeGrantOptions = props.getCodeGrantOptions();
        this.passwordGrantOptions = props.getPasswordGrantOptions();
        this.clientCredGrantOptions = props.getClientCredGrantOptions();
        this.oidcUserToPassword = props.getOidcUsers();
        this.oidcProviderName = props.getOidcProviderName();
        this.oidcApplicationType = props.getOidcApplicationType();
        this.oidcGrantType = props.getOidcGrantType();
        this.introspectionIsAvailable = props.isIntrospectionIsAvailable();
        this.keycloakAdminUrl = props.getKeycloakAdminUrl();
        this.keycloakRealms = props.getKeycloakRealms();
        this.swaggerIsAvailable = props.isSwaggerIsAvailable();
        this.graphqlIsAvailable = props.isGraphqlIsAvailable();
        this.swaggerUiPath = props.getSwaggerUiPath();
        this.graphqlUiPath = props.getGraphqlUiPath();
        this.alwaysLogoutUserInDevUiOnReload = props.isAlwaysLogoutUserInDevUiOnReload();
        this.propertiesStateId = props.getPropertiesStateId();
    }

    @NonBlocking
    public OidcDevUiRuntimePropertiesDTO getProperties() {
        return new OidcDevUiRuntimePropertiesDTO(authorizationUrl, tokenUrl, logoutUrl, config, httpPort,
                oidcProviderName, oidcApplicationType, oidcGrantType, introspectionIsAvailable, keycloakAdminUrl,
                keycloakRealms, swaggerIsAvailable, graphqlIsAvailable, swaggerUiPath, graphqlUiPath,
                alwaysLogoutUserInDevUiOnReload, propertiesStateId);
    }

    public Uni<String> exchangeCodeForTokens(String tokenUrl, String clientId, String clientSecret,
            String authorizationCode, String redirectUri) {
        return getTokens(tokenUrl, clientId, clientSecret, authorizationCode, redirectUri, vertx, codeGrantOptions)
                .ifNoItem().after(timeout).fail();
    }

    public Uni<Integer> testServiceWithToken(String token, String serviceUrl) {
        return OidcDevServicesUtils
                .testServiceWithToken(serviceUrl, token, vertx)
                .ifNoItem().after(timeout).fail();
    }

    public Uni<String> testServiceWithPassword(String tokenUrl, String serviceUrl, String clientId,
            String clientSecret, String username, String password) {
        return OidcDevServicesUtils.testServiceWithPassword(tokenUrl, serviceUrl, clientId, clientSecret, username,
                password, vertx, timeout, passwordGrantOptions, oidcUserToPassword);
    }

    public Uni<String> testServiceWithClientCred(String tokenUrl, String serviceUrl, String clientId,
            String clientSecret) {
        return OidcDevServicesUtils.testServiceWithClientCred(tokenUrl, serviceUrl, clientId, clientSecret, vertx,
                timeout, clientCredGrantOptions);
    }

}
