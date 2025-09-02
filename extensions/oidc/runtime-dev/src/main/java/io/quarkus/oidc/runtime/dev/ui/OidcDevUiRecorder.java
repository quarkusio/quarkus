package io.quarkus.oidc.runtime.dev.ui;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.oidc.common.runtime.OidcConstants;
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

    public RuntimeValue<OidcDevUiRpcSvcPropertiesBean> getRpcServiceProperties(String authorizationUrl, String tokenUrl,
            String logoutUrl, Duration webClientTimeout, Map<String, Map<String, String>> grantOptions,
            Map<String, String> oidcUsers, String oidcProviderName, String oidcApplicationType, String oidcGrantType,
            boolean introspectionIsAvailable, String keycloakAdminUrl, List<String> keycloakRealms, boolean swaggerIsAvailable,
            boolean graphqlIsAvailable, String swaggerUiPath, String graphqlUiPath, boolean alwaysLogoutUserInDevUiOnReload,
            boolean discoverMetadata, String authServerUrl, String devUiLogoutPath, String devUiReadSessionCookiePath) {
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
        return new RuntimeValue<OidcDevUiRpcSvcPropertiesBean>(
                new OidcDevUiRpcSvcPropertiesBean(authorizationUrl, tokenUrl, logoutUrl,
                        webClientTimeout, grantOptions, oidcUsers, oidcProviderName, oidcApplicationType, oidcGrantType,
                        introspectionIsAvailable, keycloakAdminUrl, keycloakRealms, swaggerIsAvailable,
                        graphqlIsAvailable, swaggerUiPath, graphqlUiPath, alwaysLogoutUserInDevUiOnReload,
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
