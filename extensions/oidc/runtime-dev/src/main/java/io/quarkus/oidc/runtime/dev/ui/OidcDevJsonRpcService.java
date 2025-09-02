package io.quarkus.oidc.runtime.dev.ui;

import static io.quarkus.oidc.runtime.dev.ui.OidcDevServicesUtils.getTokens;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

public class OidcDevJsonRpcService {
    private OidcDevUiRpcSvcPropertiesBean props;
    private VertxHttpConfig httpConfig;

    @Inject
    OidcDevLoginObserver oidcDevTokensObserver;

    @Inject
    Vertx vertx;

    @NonBlocking
    public OidcDevUiRuntimePropertiesDTO getProperties() {
        return new OidcDevUiRuntimePropertiesDTO(props.getAuthorizationUrl(), props.getTokenUrl(), props.getLogoutUrl(),
                ConfigProvider.getConfig(), httpConfig.port(),
                props.getOidcProviderName(), props.getOidcApplicationType(), props.getOidcGrantType(),
                props.isIntrospectionIsAvailable(), props.getKeycloakAdminUrl(),
                props.getKeycloakRealms(), props.isSwaggerIsAvailable(), props.isGraphqlIsAvailable(), props.getSwaggerUiPath(),
                props.getGraphqlUiPath(),
                props.isAlwaysLogoutUserInDevUiOnReload(), props.getPropertiesStateId(),
                props.getLogoutPath(), props.getReadSessionCookiePath());
    }

    public Uni<String> exchangeCodeForTokens(String tokenUrl, String clientId, String clientSecret,
            String authorizationCode, String redirectUri) {
        return getTokens(tokenUrl, clientId, clientSecret, authorizationCode, redirectUri, vertx, props.getCodeGrantOptions())
                .ifNoItem().after(props.getWebClientTimeout()).fail();
    }

    public Uni<Integer> testServiceWithToken(String token, String serviceUrl) {
        return OidcDevServicesUtils
                .testServiceWithToken(serviceUrl, token, vertx)
                .ifNoItem().after(props.getWebClientTimeout()).fail();
    }

    public Uni<String> testServiceWithPassword(String tokenUrl, String serviceUrl, String clientId,
            String clientSecret, String username, String password) {
        return OidcDevServicesUtils.testServiceWithPassword(tokenUrl, serviceUrl, clientId, clientSecret, username,
                password, vertx, props.getWebClientTimeout(), props.getPasswordGrantOptions(), props.getOidcUsers());
    }

    public Uni<String> testServiceWithClientCred(String tokenUrl, String serviceUrl, String clientId,
            String clientSecret) {
        return OidcDevServicesUtils.testServiceWithClientCred(tokenUrl, serviceUrl, clientId, clientSecret, vertx,
                props.getWebClientTimeout(), props.getClientCredGrantOptions());
    }

    public Multi<Boolean> streamOidcLoginEvent() {
        return oidcDevTokensObserver.streamOidcLoginEvent();
    }

    void hydrate(OidcDevUiRpcSvcPropertiesBean properties, VertxHttpConfig httpConfig) {
        this.props = properties;
        this.httpConfig = httpConfig;
    }
}
