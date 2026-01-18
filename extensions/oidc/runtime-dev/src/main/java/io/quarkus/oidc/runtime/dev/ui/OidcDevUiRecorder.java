package io.quarkus.oidc.runtime.dev.ui;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.runtime.OidcTenantConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

@Recorder
public class OidcDevUiRecorder {
    public static final String KEYCLOAK_URL = "keycloak.url";
    private static final Logger LOG = Logger.getLogger(OidcDevUiRecorder.class);

    private final RuntimeValue<VertxHttpConfig> httpConfig;

    public OidcDevUiRecorder(final RuntimeValue<VertxHttpConfig> httpConfig) {
        this.httpConfig = httpConfig;
    }

    public void createJsonRPCService(BeanContainer beanContainer,
            RuntimeValue<OidcDevUiRpcSvcPropertiesBean> oidcDevUiRpcSvcPropertiesBean) {
        OidcDevJsonRpcService jsonRpcService = beanContainer.beanInstance(OidcDevJsonRpcService.class);
        jsonRpcService.hydrate(oidcDevUiRpcSvcPropertiesBean.getValue(), httpConfig.getValue());
    }

    public RuntimeValue<OidcDevUiRpcSvcPropertiesBean> getRpcServiceProperties(Duration webClientTimeout,
            Map<String, Map<String, String>> grantOptions,
            String oidcProviderName, String oidcGrantType, boolean introspectionIsAvailable, boolean swaggerIsAvailable,
            boolean graphqlIsAvailable, String swaggerUiPath, String graphqlUiPath, String devServiceConfigHashCode,
            boolean discoverMetadata, String devUiLogoutPath, String devUiReadSessionCookiePath, String authServerUrl,
            String buildTimeKeycloakAdminUrl, String buildTimeOidcApplicationType) {
        var config = ConfigProvider.getConfig();
        String authorizationUrl;
        String tokenUrl;
        String logoutUrl;
        final Map<String, String> oidcUsers;
        final List<String> keycloakRealms;
        final String keycloakAdminUrl;
        final String oidcApplicationType;
        if (authServerUrl == null) {
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
        } else {
            // == OIDC Dev Services or a known provider or a test resource, or whatever user configured
            // OIDC Dev Services are not migrated yet, hence we cannot use the runtime config
            authorizationUrl = null;
            tokenUrl = null;
            logoutUrl = null;
            oidcUsers = null;
            keycloakRealms = null;
            keycloakAdminUrl = buildTimeKeycloakAdminUrl;
            oidcApplicationType = buildTimeOidcApplicationType;
        }

        if (discoverMetadata) {
            JsonObject metadata = discoverMetadata(authServerUrl);
            if (metadata != null) {
                authorizationUrl = metadata.getString("authorization_endpoint");
                tokenUrl = metadata.getString("token_endpoint");
                logoutUrl = metadata.getString("end_session_endpoint");
                introspectionIsAvailable = metadata.containsKey("introspection_endpoint")
                        || metadata.containsKey("userinfo_endpoint");
            }
        }
        return new RuntimeValue<>(
                new OidcDevUiRpcSvcPropertiesBean(authorizationUrl, tokenUrl, logoutUrl,
                        webClientTimeout, grantOptions, oidcUsers, oidcProviderName, oidcApplicationType, oidcGrantType,
                        introspectionIsAvailable, keycloakAdminUrl, keycloakRealms, swaggerIsAvailable,
                        graphqlIsAvailable, swaggerUiPath, graphqlUiPath, devServiceConfigHashCode,
                        devUiLogoutPath, devUiReadSessionCookiePath));
    }

    public Handler<RoutingContext> readSessionCookieHandler() {
        return new OidcDevSessionCookieReaderHandler();
    }

    public Handler<RoutingContext> logoutHandler() {
        return new OidcDevSessionLogoutHandler();
    }

    private static JsonObject discoverMetadata(String authServerUrl) {
        WebClient client = OidcDevServicesUtils.createWebClient(VertxCoreRecorder.getVertx().get());
        try {
            String metadataUrl = authServerUrl + OidcConstants.WELL_KNOWN_CONFIGURATION;
            LOG.infof("OIDC Dev Console: discovering the provider metadata at %s", metadataUrl);

            HttpResponse<Buffer> resp = client.getAbs(metadataUrl)
                    .putHeader(HttpHeaders.ACCEPT.toString(), "application/json").send().await().indefinitely();
            if (resp.statusCode() == 200) {
                return resp.bodyAsJsonObject();
            } else {
                LOG.errorf("OIDC metadata discovery failed: %s", resp.bodyAsString());
                return null;
            }
        } catch (Throwable t) {
            LOG.infof("OIDC metadata can not be discovered: %s", t.toString());
            return null;
        } finally {
            client.close();
        }
    }
}
