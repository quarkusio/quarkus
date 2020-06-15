package io.quarkus.it.keycloak;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantConfigResolver;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantConfigResolver implements TenantConfigResolver {
    @Override
    public OidcTenantConfig resolve(RoutingContext context) {
        String path = context.request().path();
        String tenantId = path.split("/")[2];
        if ("tenant-d".equals(tenantId)) {
            OidcTenantConfig config = new OidcTenantConfig();
            config.setTenantId("tenant-d");
            config.setAuthServerUrl(getIssuerUrl() + "/realms/quarkus-d");
            config.setClientId("quarkus-d");
            config.getCredentials().setSecret("secret");
            config.getToken().setIssuer(getIssuerUrl() + "/realms/quarkus-d");
            return config;
        } else if ("tenant-oidc".equals(tenantId)) {
            OidcTenantConfig config = new OidcTenantConfig();
            config.setTenantId("tenant-oidc");
            String uri = context.request().absoluteURI();
            String keycloakUri = path.contains("tenant-opaque")
                    ? uri.replace("/tenant-opaque/tenant-oidc/api/user", "/oidc")
                    : uri.replace("/tenant/tenant-oidc/api/user", "/oidc");
            config.setAuthServerUrl(keycloakUri);
            config.setClientId("client");
            config.getCredentials().setSecret("secret");
            return config;
        }
        return null;
    }

    private String getIssuerUrl() {
        return System.getProperty("keycloak.url", "http://localhost:8180/auth");
    }
}
