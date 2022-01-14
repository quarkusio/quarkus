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
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
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

    private static final String TENANT_ENABLED_CONFIG_KEY = CONFIG_PREFIX + "tenant-enabled";
    private static final String DISCOVERY_ENABLED_CONFIG_KEY = CONFIG_PREFIX + "discovery-enabled";
    private static final String AUTH_SERVER_URL_CONFIG_KEY = CONFIG_PREFIX + "auth-server-url";
    private static final String APP_TYPE_CONFIG_KEY = CONFIG_PREFIX + "application-type";
    private static final String SERVICE_APP_TYPE = "service";

    // Well-known providers

    private static final String KEYCLOAK = "Keycloak";
    private static final String AZURE = "Azure";
    private static final Set<String> OTHER_PROVIDERS = Set.of("Auth0", "Okta", "Google");

    OidcBuildTimeConfig oidcConfig;

    @BuildStep(onlyIf = IsDevelopment.class)
    @Consume(RuntimeConfigSetupCompleteBuildItem.class)
    void prepareOidcDevConsole(BuildProducer<DevConsoleTemplateInfoBuildItem> devConsoleInfo,
            BuildProducer<DevConsoleRuntimeTemplateInfoBuildItem> devConsoleRuntimeInfo,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            BuildProducer<DevConsoleRouteBuildItem> devConsoleRoute,
            Capabilities capabilities, CurateOutcomeBuildItem curateOutcomeBuildItem) {
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

            String authServerUrl = null;
            try {
                authServerUrl = getConfigProperty(AUTH_SERVER_URL_CONFIG_KEY);
            } catch (Exception ex) {
                // It is not possible to initialize OIDC Dev Console UI without being able to access this property at the build time
                return;
            }
            JsonObject metadata = null;
            if (isDiscoveryEnabled()) {
                metadata = discoverMetadata(authServerUrl);
                if (metadata == null) {
                    return;
                }
            }
            String providerName = tryToGetProviderName(authServerUrl);
            if (KEYCLOAK.equals(providerName)) {
                devConsoleInfo.produce(new DevConsoleTemplateInfoBuildItem("keycloakAdminUrl",
                        authServerUrl.substring(0, authServerUrl.indexOf("/realms/"))));
            }
            produceDevConsoleTemplateItems(capabilities,
                    devConsoleInfo,
                    devConsoleRuntimeInfo,
                    curateOutcomeBuildItem,
                    providerName,
                    getApplicationType(),
                    oidcConfig.devui.grant.type.isPresent() ? oidcConfig.devui.grant.type.get().getGrantType() : "code",
                    metadata != null ? metadata.getString("authorization_endpoint") : null,
                    metadata != null ? metadata.getString("token_endpoint") : null,
                    metadata != null ? metadata.getString("end_session_endpoint") : null,
                    metadata != null ? metadata.containsKey("introspection_endpoint") : false);

            Duration webClientTimeout = oidcConfig.devui.webClienTimeout.isPresent() ? oidcConfig.devui.webClienTimeout.get()
                    : Duration.ofSeconds(4);
            produceDevConsoleRouteItems(devConsoleRoute,
                    new OidcTestServiceHandler(vertxInstance, webClientTimeout),
                    new OidcAuthorizationCodePostHandler(vertxInstance, webClientTimeout, oidcConfig.devui.grantOptions),
                    new OidcPasswordClientCredHandler(vertxInstance, webClientTimeout, oidcConfig.devui.grantOptions));
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
        return getBooleanProperty(TENANT_ENABLED_CONFIG_KEY);
    }

    private static boolean isDiscoveryEnabled() {
        return getBooleanProperty(DISCOVERY_ENABLED_CONFIG_KEY);
    }

    private static boolean getBooleanProperty(String name) {
        return ConfigProvider.getConfig().getOptionalValue(name, Boolean.class).orElse(true);
    }

    private static boolean isClientIdSet() {
        return ConfigUtils.isPropertyPresent(CLIENT_ID_CONFIG_KEY);
    }

    private static boolean isAuthServerUrlSet() {
        return ConfigUtils.isPropertyPresent(AUTH_SERVER_URL_CONFIG_KEY);
    }

    private static String getApplicationType() {
        return ConfigProvider.getConfig().getOptionalValue(APP_TYPE_CONFIG_KEY, String.class).orElse(SERVICE_APP_TYPE);
    }

}
