package io.quarkus.oidc.test;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.TenantConfigResolver;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantConfigResolver implements TenantConfigResolver {
    @Override
    public OidcTenantConfig resolve(RoutingContext context) {
        if (context.request().path().endsWith("/tenant-config-resolver")) {
            OidcTenantConfig config = new OidcTenantConfig();
            config.setTenantId("tenant-config-resolver");
            config.setAuthServerUrl(getIssuerUrl() + "/realms/devmode");
            config.setClientId("client-dev-mode");
            config.applicationType = ApplicationType.WEB_APP;
            return config;
        }
        return null;
    }

    private String getIssuerUrl() {
        return System.getProperty("keycloak.url", "http://localhost:8180/auth");
    }
}
