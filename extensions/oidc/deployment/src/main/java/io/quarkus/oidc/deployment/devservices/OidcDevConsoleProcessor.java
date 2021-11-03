package io.quarkus.oidc.deployment.devservices;

import java.time.Duration;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.RuntimeConfigSetupCompleteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.deployment.OidcBuildTimeConfig;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

public class OidcDevConsoleProcessor extends AbstractDevConsoleProcessor {
    static volatile Vertx vertxInstance;
    private static final Logger LOG = Logger.getLogger(OidcDevConsoleProcessor.class);

    private static final String CONFIG_PREFIX = "quarkus.oidc.";
    private static final String TENANT_ENABLED_CONFIG_KEY = CONFIG_PREFIX + "tenant-enabled";
    private static final String AUTH_SERVER_URL_CONFIG_KEY = CONFIG_PREFIX + "auth-server-url";
    private static final String APP_TYPE_CONFIG_KEY = CONFIG_PREFIX + "application-type";
    private static final String SERVICE_APP_TYPE = "service";
    private static final String CLIENT_ID_CONFIG_KEY = CONFIG_PREFIX + "client-id";
    private static final String CLIENT_SECRET_CONFIG_KEY = CONFIG_PREFIX + "credentials.secret";

    // Well-known providers

    private static final String KEYCLOAK = "Keycloak";
    private static final String AZURE = "Azure";
    private static final Set<String> OTHER_PROVIDERS = Set.of("Auth0", "Okta", "Google");

    OidcBuildTimeConfig oidcConfig;

    @BuildStep(onlyIf = IsDevelopment.class)
    @Consume(RuntimeConfigSetupCompleteBuildItem.class)
    void prepareOidcDevConsole(BuildProducer<DevConsoleTemplateInfoBuildItem> console,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            BuildProducer<DevConsoleRouteBuildItem> devConsoleRoute,
            Capabilities capabilities) {
        if (isOidcTenantEnabled() && isAuthServerUrlSet() && isClientIdSet()) {

            if (vertxInstance == null) {
                vertxInstance = Vertx.vertx();

                Runnable closeTask = new Runnable() {
                    @Override
                    public void run() {
                        if (vertxInstance != null) {
                            try {
                                vertxInstance.close();
                            } catch (Throwable t) {
                                LOG.error("Failed to close Vertx instance", t);
                            }
                        }
                        vertxInstance = null;
                    }
                };
                closeBuildItem.addCloseTask(closeTask, true);
            }

            String authServerUrl = getConfigProperty(AUTH_SERVER_URL_CONFIG_KEY);
            JsonObject metadata = discoverMetadata(authServerUrl);
            if (metadata == null) {
                return;
            }
            String providerName = tryToGetProviderName(authServerUrl);
            if (KEYCLOAK.equals(providerName)) {
                console.produce(new DevConsoleTemplateInfoBuildItem("keycloakAdminUrl",
                        authServerUrl.substring(0, authServerUrl.indexOf("/realms/"))));
            }
            produceDevConsoleTemplateItems(capabilities,
                    console,
                    providerName,
                    getApplicationType(),
                    oidcConfig.devui.grant.type.isPresent() ? oidcConfig.devui.grant.type.get().getGrantType() : "code",
                    getConfigProperty(CLIENT_ID_CONFIG_KEY),
                    getClientSecret(),
                    metadata.getString("authorization_endpoint"),
                    metadata.getString("token_endpoint"),
                    metadata.getString("end_session_endpoint"),
                    metadata.containsKey("introspection_endpoint"));

            Duration webClientTimeout = oidcConfig.devui.webClienTimeout.isPresent() ? oidcConfig.devui.webClienTimeout.get()
                    : Duration.ofSeconds(4);
            produceDevConsoleRouteItems(devConsoleRoute,
                    new OidcTestServiceHandler(vertxInstance, webClientTimeout),
                    new OidcAuthorizationCodePostHandler(vertxInstance, webClientTimeout),
                    new OidcPasswordClientCredHandler(vertxInstance, webClientTimeout));
        }
    }

    private String tryToGetProviderName(String authServerUrl) {
        if (authServerUrl.contains("/realms/")) {
            return KEYCLOAK;
        }
        if (authServerUrl.contains("microsoft")) {
            return AZURE;
        }
        for (String provider : OTHER_PROVIDERS) {
            if (authServerUrl.contains(provider.toLowerCase())) {
                return provider;
            }
        }
        return null;
    }

    private JsonObject discoverMetadata(String authServerUrl) {
        WebClient client = OidcDevServicesUtils.createWebClient(vertxInstance);
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

    private String getConfigProperty(String name) {
        return ConfigProvider.getConfig().getValue(name, String.class);
    }

    private static boolean isOidcTenantEnabled() {
        return ConfigProvider.getConfig().getOptionalValue(TENANT_ENABLED_CONFIG_KEY, Boolean.class).orElse(true);
    }

    private static boolean isClientIdSet() {
        return ConfigUtils.isPropertyPresent(CLIENT_ID_CONFIG_KEY);
    }

    private static String getClientSecret() {
        return ConfigProvider.getConfig().getOptionalValue(CLIENT_SECRET_CONFIG_KEY, String.class).orElse("");
    }

    private static boolean isAuthServerUrlSet() {
        return ConfigUtils.isPropertyPresent(AUTH_SERVER_URL_CONFIG_KEY);
    }

    private static String getApplicationType() {
        return ConfigProvider.getConfig().getOptionalValue(APP_TYPE_CONFIG_KEY, String.class).orElse(SERVICE_APP_TYPE);
    }

}
