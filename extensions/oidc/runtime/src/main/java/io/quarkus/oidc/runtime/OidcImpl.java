package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType.SERVICE;
import static io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType.WEB_APP;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.quarkus.oidc.Oidc;
import io.quarkus.oidc.OidcTenantConfig;

final class OidcImpl implements Oidc {

    private final Map<String, OidcTenantConfig> staticTenantConfigs;
    private OidcTenantConfig defaultTenantConfig;

    OidcImpl(OidcConfig config) {
        this.defaultTenantConfig = OidcTenantConfig.of(OidcConfig.getDefaultTenant(config));
        this.staticTenantConfigs = getStaticTenants(config);
    }

    @Override
    public void create(OidcTenantConfig tenantConfig) {
        Objects.requireNonNull(tenantConfig);
        putStaticTenantConfig(tenantConfig);
    }

    @Override
    public void createServiceApp(String authServerUrl) {
        create(OidcTenantConfig.authServerUrl(authServerUrl).applicationType(SERVICE).build());
    }

    @Override
    public void createWebApp(String authServerUrl, String clientId, String clientSecret) {
        create(OidcTenantConfig.authServerUrl(authServerUrl).clientId(clientId).applicationType(WEB_APP)
                .credentials(clientSecret).build());
    }

    Map<String, OidcTenantConfig> getStaticTenantConfigs() {
        return Collections.unmodifiableMap(staticTenantConfigs);
    }

    OidcTenantConfig getDefaultTenantConfig() {
        return defaultTenantConfig;
    }

    private void putStaticTenantConfig(OidcTenantConfig tenantConfig) {
        final String tenantId = tenantConfig.tenantId().get();
        if (defaultTenantConfig.tenantId().get().equals(tenantId)) {
            defaultTenantConfig = tenantConfig;
        } else {
            staticTenantConfigs.put(tenantId, tenantConfig);
        }
    }

    private static Map<String, OidcTenantConfig> getStaticTenants(OidcConfig config) {
        Map<String, OidcTenantConfig> tenantConfigs = new HashMap<>();
        for (var tenant : config.namedTenants().entrySet()) {
            String tenantKey = tenant.getKey();
            if (OidcConfig.DEFAULT_TENANT_KEY.equals(tenantKey)) {
                continue;
            }
            var namedTenantConfig = OidcTenantConfig.of(tenant.getValue());
            tenantConfigs.put(tenantKey, namedTenantConfig);
        }
        return tenantConfigs;
    }
}
