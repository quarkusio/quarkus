package io.quarkus.oidc.deployment.devservices;

import java.util.Optional;
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
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.OidcTenantConfig.Provider;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.deployment.OidcBuildTimeConfig;
import io.quarkus.oidc.runtime.providers.KnownOidcProviders;
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
    private static final String OIDC_PROVIDER_CONFIG_KEY = "quarkus.oidc.provider";
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
        if (!isOidcTenantEnabled() || !isClientIdSet()) {
            return;
        }
        final OidcTenantConfig providerConfig = getProviderConfig();
        final String authServerUrl = getAuthServerUrl(providerConfig);
        if (authServerUrl != null) {

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
                    getApplicationType(providerConfig),
                    oidcConfig.devui.grant.type.isPresent() ? oidcConfig.devui.grant.type.get().getGrantType() : "code",
                    metadata != null ? metadata.getString("authorization_endpoint") : null,
                    metadata != null ? metadata.getString("token_endpoint") : null,
                    metadata != null ? metadata.getString("end_session_endpoint") : null,
                    metadata != null ? metadata.containsKey("introspection_endpoint")
                            || metadata.containsKey("userinfo_endpoint") : false);

            produceDevConsoleRouteItems(devConsoleRoute,
                    new OidcTestServiceHandler(vertxInstance, oidcConfig.devui.webClientTimeout),
                    new OidcAuthorizationCodePostHandler(vertxInstance, oidcConfig.devui.webClientTimeout,
                            oidcConfig.devui.grantOptions),
                    new OidcPasswordClientCredHandler(vertxInstance, oidcConfig.devui.webClientTimeout,
                            oidcConfig.devui.grantOptions));
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

    private static String getConfigProperty(String name) {
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

    private static String getAuthServerUrl(OidcTenantConfig providerConfig) {
        try {
            return getConfigProperty(AUTH_SERVER_URL_CONFIG_KEY);
        } catch (Exception ex) {
            return providerConfig != null ? providerConfig.authServerUrl.get() : null;
        }
    }

    private static String getApplicationType(OidcTenantConfig providerConfig) {
        Optional<ApplicationType> appType = ConfigProvider.getConfig().getOptionalValue(APP_TYPE_CONFIG_KEY,
                ApplicationType.class);
        if (appType.isEmpty() && providerConfig != null) {
            appType = providerConfig.applicationType;
        }
        return appType.isPresent() ? appType.get().name().toLowerCase() : SERVICE_APP_TYPE;
    }

    private static OidcTenantConfig getProviderConfig() {
        try {
            Provider p = ConfigProvider.getConfig().getValue(OIDC_PROVIDER_CONFIG_KEY, Provider.class);
            return KnownOidcProviders.provider(p);
        } catch (Exception ex) {
            return null;
        }

    }

}
