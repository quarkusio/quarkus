package io.quarkus.oidc.deployment.devservices;

import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.IsLocalDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.RuntimeConfigSetupCompleteBuildItem;
import io.quarkus.devservices.oidc.OidcDevServicesConfigBuildItem;
import io.quarkus.devui.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.CardPageBuildItem;
import io.quarkus.oidc.deployment.OidcBuildTimeConfig;
import io.quarkus.oidc.runtime.OidcTenantConfig;
import io.quarkus.oidc.runtime.OidcTenantConfig.Provider;
import io.quarkus.oidc.runtime.dev.ui.OidcDevJsonRpcService;
import io.quarkus.oidc.runtime.dev.ui.OidcDevUiRecorder;
import io.quarkus.oidc.runtime.providers.KnownOidcProviders;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;

public class OidcDevUIProcessor extends AbstractDevUIProcessor {

    private static final String TENANT_ENABLED_CONFIG_KEY = CONFIG_PREFIX + "tenant-enabled";
    private static final String DISCOVERY_ENABLED_CONFIG_KEY = CONFIG_PREFIX + "discovery-enabled";
    private static final String AUTH_SERVER_URL_CONFIG_KEY = CONFIG_PREFIX + "auth-server-url";
    private static final String OIDC_PROVIDER_CONFIG_KEY = "quarkus.oidc.provider";

    // Well-known providers

    private static final String KEYCLOAK = "Keycloak";
    private static final String ENTRAID = "Microsoft Entra ID";
    private static final Set<String> OTHER_PROVIDERS = Set.of("Auth0", "Okta", "Google", "Github", "Spotify");

    OidcBuildTimeConfig oidcConfig;

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = IsLocalDevelopment.class)
    @Consume(CoreVertxBuildItem.class) // metadata discovery requires Vertx instance
    @Consume(RuntimeConfigSetupCompleteBuildItem.class)
    void prepareOidcDevConsole(Capabilities capabilities,
            BeanContainerBuildItem beanContainer,
            NonApplicationRootPathBuildItem nonApplicationRootPathBuildItem,
            BuildProducer<CardPageBuildItem> cardPageProducer,
            OidcDevUiRecorder recorder,
            Optional<OidcDevServicesConfigBuildItem> oidcDevServicesConfigBuildItem) {
        if (!isOidcTenantEnabled() || (!isClientIdSet() && oidcDevServicesConfigBuildItem.isEmpty())) {
            return;
        }
        final OidcTenantConfig providerConfig = getProviderConfig();
        final boolean oidcDevServicesEnabled = oidcDevServicesConfigBuildItem.isPresent();
        final String authServerUrl = oidcDevServicesEnabled
                ? oidcDevServicesConfigBuildItem.get().getConfig().get(AUTH_SERVER_URL_CONFIG_KEY)
                : getAuthServerUrl(providerConfig);
        if (authServerUrl != null) {
            boolean discoverMetadata = isDiscoveryEnabled(providerConfig);
            String providerName = tryToGetProviderName(authServerUrl);

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
                    null,
                    null,
                    null,
                    checkProviderUserInfoRequired(providerConfig),
                    beanContainer,
                    oidcConfig.devui().webClientTimeout(),
                    oidcConfig.devui().grantOptions(),
                    nonApplicationRootPathBuildItem,
                    keycloakAdminUrl,
                    null,
                    null,
                    true,
                    discoverMetadata,
                    authServerUrl);
            cardPageProducer.produce(cardPage);
        }
    }

    @BuildStep(onlyIf = IsLocalDevelopment.class)
    JsonRPCProvidersBuildItem produceOidcDevJsonRpcService() {
        return new JsonRPCProvidersBuildItem(OidcDevJsonRpcService.class);
    }

    private static boolean checkProviderUserInfoRequired(OidcTenantConfig providerConfig) {
        if (providerConfig != null) {
            return providerConfig.authentication().userInfoRequired().orElse(false);
        }
        return false;
    }

    private static String tryToGetProviderName(String authServerUrl) {
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

    private static String getConfigProperty(String name) {
        return ConfigProvider.getConfig().getValue(name, String.class);
    }

    private static boolean isOidcTenantEnabled() {
        return getBooleanProperty(TENANT_ENABLED_CONFIG_KEY);
    }

    private static boolean isDiscoveryEnabled(OidcTenantConfig providerConfig) {
        return ConfigProvider.getConfig().getOptionalValue(DISCOVERY_ENABLED_CONFIG_KEY, Boolean.class)
                .orElse((providerConfig != null ? providerConfig.discoveryEnabled().orElse(true) : true));
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
            return providerConfig != null ? providerConfig.authServerUrl().get() : null;
        }
    }

    private static OidcTenantConfig getProviderConfig() {
        try {
            return ConfigProvider.getConfig()
                    .getOptionalValue(OIDC_PROVIDER_CONFIG_KEY, Provider.class)
                    .map(KnownOidcProviders::provider)
                    .orElse(null);
        } catch (Exception ex) {
            return null;
        }

    }

}
