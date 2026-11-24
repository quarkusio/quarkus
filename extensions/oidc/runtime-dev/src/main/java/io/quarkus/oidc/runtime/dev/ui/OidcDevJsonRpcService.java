package io.quarkus.oidc.runtime.dev.ui;

import static io.quarkus.oidc.runtime.dev.ui.OidcDevServicesUtils.getTokens;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.runtime.OidcTenantConfig;
import io.quarkus.oidc.runtime.dev.ui.OidcDevUiBuildTimeProperties.DevServiceType;
import io.quarkus.vertx.http.HttpServer;
import io.smallrye.config.Config;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.ext.web.client.WebClient;

public class OidcDevJsonRpcService {
    private static final String KEYCLOAK_URL = "keycloak.url";
    private static final Logger LOG = Logger.getLogger(OidcDevJsonRpcService.class);

    private volatile Uni<OidcDevUiRpcSvcPropertiesBean> propertiesBeanUni;
    private volatile OidcDevUiRpcSvcPropertiesBean resolvedProperties;
    private volatile Vertx vertx;

    @Inject
    OidcDevLoginObserver oidcDevTokensObserver;

    @Inject
    HttpServer httpServer;

    public Uni<String> getAdminConsoleUrl() {
        return withProperties(props -> Uni.createFrom().item(props.getKeycloakAdminUrl()));
    }

    public Uni<OidcDevUiRuntimePropertiesDTO> getProperties() {
        return withProperties(props -> Uni.createFrom().item(new OidcDevUiRuntimePropertiesDTO(props.getAuthorizationUrl(),
                props.getTokenUrl(), props.getLogoutUrl(), Config.get(), httpServer.getPort(),
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

    public void hydrate(OidcDevUiBuildTimeProperties buildTimeProperties, Vertx vertx) {
        this.vertx = vertx;
        this.propertiesBeanUni = getRpcServiceProperties(buildTimeProperties, vertx);
    }

    private static Uni<OidcDevUiRpcSvcPropertiesBean> getRpcServiceProperties(OidcDevUiBuildTimeProperties p, Vertx vertx) {
        Config config = Config.get();
        String authorizationUrl;
        String tokenUrl;
        String logoutUrl;
        final Map<String, String> oidcUsers;
        final List<String> keycloakRealms;
        final String keycloakAdminUrl;
        final String oidcApplicationType;
        boolean introspectionIsAvailable = p.introspectionIsAvailable();
        if (p.devServiceType() == DevServiceType.KEYCLOAK) {
            // == Keycloak Dev Services

            // the "keycloak.auth-server-internal-url" is in most cases 'quarkus.oidc.auth-server-url'
            // however, if the OIDC extension is disabled, but the Keycloak Dev Service is running in a DEV mode
            // for example because it is required by the OIDC Client, the 'quarkus.oidc.auth-server-url' will not be present
            // TODO: eventually, we should probably move this DEV UI to a separate component shared by all the extensions
            //  that may require keycloak dev service in DEV MODE
            String realmUrl = config.getValue("keycloak.auth-server-internal-url", String.class);

            authorizationUrl = realmUrl + "/protocol/openid-connect/auth";
            tokenUrl = realmUrl + "/protocol/openid-connect/token";
            logoutUrl = realmUrl + "/protocol/openid-connect/logout";
            if (config.getOptionalValue("oidc.users", String.class).isPresent()) {
                oidcUsers = config.getValues("oidc.users", String.class).stream().map(item -> {
                    String[] usernameToPassword = item.split("=");
                    return Map.entry(usernameToPassword[0], usernameToPassword[1]);
                }).collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
            } else {
                oidcUsers = Map.of();
            }
            if (config.getOptionalValue("keycloak.realms", String.class).isPresent()) {
                keycloakRealms = config.getValues("keycloak.realms", String.class);
            } else {
                keycloakRealms = List.of();
            }
            keycloakAdminUrl = config.getValue(KEYCLOAK_URL, String.class);
            oidcApplicationType = config
                    .getOptionalValue("quarkus.oidc.application-type", OidcTenantConfig.ApplicationType.class)
                    // constant is "WEB_APP" while documented value is "web-app" and we expect users to use "web-app"
                    // if this get changed, we need to update qwc-oidc-provider.js as well
                    .map(at -> OidcTenantConfig.ApplicationType.WEB_APP == at ? "web-app"
                            : at.name().toLowerCase())
                    .orElse(OidcTenantConfig.ApplicationType.SERVICE.name().toLowerCase());
        } else if (p.devServiceType() == DevServiceType.OIDC) {
            var oidcAuthServerUrl = config.getValue("quarkus.oidc.auth-server-url", String.class);
            authorizationUrl = oidcAuthServerUrl + "/authorize";
            tokenUrl = oidcAuthServerUrl + "/token";
            logoutUrl = oidcAuthServerUrl + "/logout";
            oidcUsers = null;
            keycloakRealms = null;
            keycloakAdminUrl = null;
            introspectionIsAvailable = true;
            oidcApplicationType = config.getOptionalValue("quarkus.oidc.application-type", String.class).orElse("service");
        } else {
            // == known provider or a test resource, or whatever user configured
            authorizationUrl = null;
            tokenUrl = null;
            logoutUrl = null;
            oidcUsers = null;
            keycloakRealms = null;
            keycloakAdminUrl = p.buildTimeKeycloakAdminUrl();
            oidcApplicationType = p.buildTimeOidcApplicationType();
        }

        final var beanWithoutDiscovery = new OidcDevUiRpcSvcPropertiesBean(authorizationUrl, tokenUrl, logoutUrl,
                p.webClientTimeout(), p.grantOptions(), oidcUsers, p.oidcProviderName(), oidcApplicationType,
                p.oidcGrantType(), introspectionIsAvailable, keycloakAdminUrl, keycloakRealms, p.swaggerIsAvailable(),
                p.graphqlIsAvailable(), p.swaggerUiPath(), p.graphqlUiPath(), p.devServiceConfigHashCode(),
                p.devUiLogoutPath(), p.devUiReadSessionCookiePath());
        if (p.discoverMetadata()) {
            return discoverMetadata(vertx, p.authServerUrl(), p.webClientTimeout())
                    .map(metadata -> {
                        if (metadata != null) {
                            return new OidcDevUiRpcSvcPropertiesBean(metadata.getString("authorization_endpoint"),
                                    metadata.getString("token_endpoint"), metadata.getString("end_session_endpoint"),
                                    p.webClientTimeout(), p.grantOptions(), oidcUsers, p.oidcProviderName(),
                                    oidcApplicationType, p.oidcGrantType(),
                                    metadata.containsKey("introspection_endpoint") || metadata.containsKey("userinfo_endpoint"),
                                    keycloakAdminUrl, keycloakRealms, p.swaggerIsAvailable(),
                                    p.graphqlIsAvailable(), p.swaggerUiPath(), p.graphqlUiPath(), p.devServiceConfigHashCode(),
                                    p.devUiLogoutPath(), p.devUiReadSessionCookiePath());
                        } else {
                            return beanWithoutDiscovery;
                        }
                    })
                    .memoize().indefinitely();
        } else {
            return Uni.createFrom().item(beanWithoutDiscovery);
        }
    }

    private static Uni<JsonObject> discoverMetadata(Vertx vertx, String authServerUrl, Duration webClientTimeout) {
        return Uni.createFrom().deferred(() -> {
            WebClient client = OidcDevServicesUtils.createWebClient(vertx);

            String metadataUrl = authServerUrl + OidcConstants.WELL_KNOWN_CONFIGURATION;

            LOG.infof("OIDC Dev Console: discovering the provider metadata at %s", metadataUrl);

            return client.getAbs(metadataUrl)
                    .putHeader(HttpHeaders.ACCEPT.toString(), "application/json")
                    .timeout(webClientTimeout.toMillis())
                    .send()
                    .map(resp -> {
                        if (resp.statusCode() == 200) {
                            return resp.bodyAsJsonObject();
                        } else {
                            LOG.errorf("OIDC metadata discovery failed: %s", resp.bodyAsString());
                            return null;
                        }
                    })
                    .onFailure().invoke(t -> LOG.infof("OIDC metadata can not be discovered: %s", t.toString()))
                    .onFailure().recoverWithNull()
                    .onTermination().invoke(client::close);
        });
    }
}
