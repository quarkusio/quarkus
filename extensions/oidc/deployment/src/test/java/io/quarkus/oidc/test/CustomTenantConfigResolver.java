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
            config.setAuthServerUrl(getIssuerUrl() + "/realms/quarkus");
            config.setClientId("quarkus-web-app");
            config.getCredentials().setSecret("secret");
            config.applicationType = ApplicationType.WEB_APP;
            return config;
        }
        return null;
    }

    private String getIssuerUrl() {
        return System.getProperty("keycloak.url");
    }
}
