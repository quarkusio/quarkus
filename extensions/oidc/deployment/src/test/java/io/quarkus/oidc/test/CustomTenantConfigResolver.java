package io.quarkus.oidc.test;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.TenantConfigResolver;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantConfigResolver implements TenantConfigResolver {
    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext context, OidcRequestContext<OidcTenantConfig> requestContext) {
        if (context.request().path().endsWith("/tenant-config-resolver")) {
            OidcTenantConfig config = new OidcTenantConfig();
            config.setTenantId("tenant-config-resolver");
            config.setAuthServerUrl(getIssuerUrl() + "/realms/quarkus");
            config.setClientId("quarkus-web-app");
            config.getCredentials().setSecret("secret");
            config.setApplicationType(ApplicationType.WEB_APP);
            return Uni.createFrom().item(config);
        }
        return Uni.createFrom().nullItem();
    }

    private String getIssuerUrl() {
        return System.getProperty("keycloak.url");
    }
}
