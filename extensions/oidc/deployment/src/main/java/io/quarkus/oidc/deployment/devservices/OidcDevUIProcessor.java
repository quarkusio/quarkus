package io.quarkus.oidc.deployment.devservices;

import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ConfigurationBuildItem;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.RuntimeConfigSetupCompleteBuildItem;
import io.quarkus.devservices.oidc.OidcDevServicesConfigBuildItem;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.OidcTenantConfig.Provider;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.deployment.OidcBuildTimeConfig;
import io.quarkus.oidc.runtime.devui.OidcDevJsonRpcService;
import io.quarkus.oidc.runtime.devui.OidcDevServicesUtils;
import io.quarkus.oidc.runtime.devui.OidcDevUiRecorder;
import io.quarkus.oidc.runtime.providers.KnownOidcProviders;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.runtime.HttpConfiguration;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

public class OidcDevUIProcessor extends AbstractDevUIProcessor {
    static volatile Vertx vertxInstance;
    private static final Logger LOG = Logger.getLogger(OidcDevUIProcessor.class);

    private static final String TENANT_ENABLED_CONFIG_KEY = CONFIG_PREFIX + "tenant-enabled";
    private static final String DISCOVERY_ENABLED_CONFIG_KEY = CONFIG_PREFIX + "discovery-enabled";
    private static final String AUTH_SERVER_URL_CONFIG_KEY = CONFIG_PREFIX + "auth-server-url";
    private static final String APP_TYPE_CONFIG_KEY = CONFIG_PREFIX + "application-type";
    private static final String OIDC_PROVIDER_CONFIG_KEY = "quarkus.oidc.provider";
    private static final String SERVICE_APP_TYPE = "service";

    // Well-known providers

    private static final String KEYCLOAK = "Keycloak";
    private static final String ENTRAID = "Microsoft Entra ID";
    private static final Set<String> OTHER_PROVIDERS = Set.of("Auth0", "Okta", "Google", "Github", "Spotify");

    OidcBuildTimeConfig oidcConfig;

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = IsDevelopment.class)
    @Consume(RuntimeConfigSetupCompleteBuildItem.class)
    void prepareOidcDevConsole(CuratedApplicationShutdownBuildItem closeBuildItem,
            Capabilities capabilities,
            HttpConfiguration httpConfiguration,
            BeanContainerBuildItem beanContainer,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            BuildProducer<CardPageBuildItem> cardPageProducer,
            ConfigurationBuildItem configurationBuildItem,
            OidcDevUiRecorder recorder,
            Optional<OidcDevServicesConfigBuildItem> oidcDevServicesConfigBuildItem) {
        if (!isOidcTenantEnabled() || (!isClientIdSet() && oidcDevServicesConfigBuildItem.isEmpty())) {
            return;
        }
        final OidcTenantConfig providerConfig = getProviderConfig();
        final String authServerUrl = oidcDevServicesConfigBuildItem.isPresent()
                ? oidcDevServicesConfigBuildItem.get().getConfig().get(AUTH_SERVER_URL_CONFIG_KEY)
                : getAuthServerUrl(providerConfig);
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
            if (isDiscoveryEnabled(providerConfig)) {
                metadata = discoverMetadata(authServerUrl);
                if (metadata == null) {
                    return;
                }
            }
            String providerName = tryToGetProviderName(authServerUrl);
            boolean metadataNotNull = metadata != null;

            final String keycloakAdminUrl;
            if (KEYCLOAK.equals(providerName)) {
                keycloakAdminUrl = authServerUrl.substring(0, authServerUrl.indexOf("/realms/"));
            } else {
                keycloakAdminUrl = null;
            }
            var cardPage = createProviderWebComponent(recorder,
                    capabilities,
                    providerName,
                    getApplicationType(providerConfig),
                    oidcConfig.devui().grant().type().isPresent() ? oidcConfig.devui().grant().type().get().getGrantType()
                            : "code",
                    metadataNotNull ? metadata.getString("authorization_endpoint") : null,
                    metadataNotNull ? metadata.getString("token_endpoint") : null,
                    metadataNotNull ? metadata.getString("end_session_endpoint") : null,
                    metadataNotNull
                            ? (metadata.containsKey("introspection_endpoint") || metadata.containsKey("userinfo_endpoint"))
                            : checkProviderUserInfoRequired(providerConfig),
                    beanContainer,
                    oidcConfig.devui().webClientTimeout(),
                    oidcConfig.devui().grantOptions(),
                    nonApplicationRootPathBuildItem,
                    configurationBuildItem,
                    keycloakAdminUrl,
                    null,
                    null,
                    true,
                    httpConfiguration);
            cardPageProducer.produce(cardPage);
        }
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    JsonRPCProvidersBuildItem produceOidcDevJsonRpcService() {
        return new JsonRPCProvidersBuildItem(OidcDevJsonRpcService.class);
    }

    private boolean checkProviderUserInfoRequired(OidcTenantConfig providerConfig) {
        if (providerConfig != null) {
            return providerConfig.authentication.userInfoRequired.orElse(false);
        }
        return false;
    }

    private String tryToGetProviderName(String authServerUrl) {
        if (authServerUrl.contains("/realms/")) {
            return KEYCLOAK;
        }
        if (authServerUrl.contains("microsoft")) {
            return ENTRAID;
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

    private static boolean isDiscoveryEnabled(OidcTenantConfig providerConfig) {
        return ConfigProvider.getConfig().getOptionalValue(DISCOVERY_ENABLED_CONFIG_KEY, Boolean.class)
                .orElse((providerConfig != null ? providerConfig.discoveryEnabled.orElse(true) : true));
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
