package io.quarkus.oidc.runtime.dev.ui;

import static io.quarkus.oidc.runtime.dev.ui.OidcDevServicesUtils.getTokens;

import java.util.function.Function;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

public class OidcDevJsonRpcService {
    private Uni<OidcDevUiRpcSvcPropertiesBean> propertiesBeanUni;
    private volatile OidcDevUiRpcSvcPropertiesBean resolvedProperties;
    private VertxHttpConfig httpConfig;

    @Inject
    OidcDevLoginObserver oidcDevTokensObserver;

    @Inject
    Vertx vertx;

    public Uni<String> getAdminConsoleUrl() {
        return withProperties(props -> Uni.createFrom().item(props.getKeycloakAdminUrl()));
    }

    public Uni<OidcDevUiRuntimePropertiesDTO> getProperties() {
        return withProperties(props -> Uni.createFrom().item(new OidcDevUiRuntimePropertiesDTO(props.getAuthorizationUrl(),
                props.getTokenUrl(), props.getLogoutUrl(), ConfigProvider.getConfig(), httpConfig.port(),
                props.getOidcProviderName(), props.getOidcApplicationType(), props.getOidcGrantType(),
                props.isIntrospectionIsAvailable(), props.getKeycloakAdminUrl(),
                props.getKeycloakRealms(), props.isSwaggerIsAvailable(), props.isGraphqlIsAvailable(), props.getSwaggerUiPath(),
                props.getGraphqlUiPath(), props.getDevServiceConfigHashCode(), props.getPropertiesStateId(),
                props.getLogoutPath(), props.getReadSessionCookiePath())));
    }

    public Uni<String> exchangeCodeForTokens(String tokenUrl, String clientId, String clientSecret,
            String authorizationCode, String redirectUri) {
        return withProperties(props -> getTokens(tokenUrl, clientId, clientSecret, authorizationCode, redirectUri,
                vertx, props.getCodeGrantOptions()).ifNoItem().after(props.getWebClientTimeout()).fail());
    }

    public Uni<Integer> testServiceWithToken(String token, String serviceUrl) {
        return withProperties(props -> OidcDevServicesUtils
                .testServiceWithToken(serviceUrl, token, vertx)
                .ifNoItem().after(props.getWebClientTimeout()).fail());
    }

    public Uni<String> testServiceWithPassword(String tokenUrl, String serviceUrl, String clientId,
            String clientSecret, String username, String password) {
        return withProperties(
                props -> OidcDevServicesUtils.testServiceWithPassword(tokenUrl, serviceUrl, clientId, clientSecret, username,
                        password, vertx, props.getWebClientTimeout(), props.getPasswordGrantOptions(), props.getOidcUsers()));
    }

    public Uni<String> testServiceWithClientCred(String tokenUrl, String serviceUrl, String clientId,
            String clientSecret) {
        return withProperties(props -> OidcDevServicesUtils.testServiceWithClientCred(tokenUrl,
                serviceUrl, clientId, clientSecret, vertx, props.getWebClientTimeout(), props.getClientCredGrantOptions()));
    }

    public Multi<Boolean> streamOidcLoginEvent() {
        return oidcDevTokensObserver.streamOidcLoginEvent();
    }

    private <T> Uni<T> withProperties(Function<OidcDevUiRpcSvcPropertiesBean, Uni<T>> function) {
        if (resolvedProperties == null) {
            return propertiesBeanUni.flatMap(props -> {
                this.resolvedProperties = props;
                return function.apply(props);
            });
        }
        return function.apply(resolvedProperties);
    }

    void hydrate(Uni<OidcDevUiRpcSvcPropertiesBean> propertiesBeanUni, VertxHttpConfig httpConfig) {
        this.propertiesBeanUni = propertiesBeanUni;
        this.httpConfig = httpConfig;
    }
}
