package io.quarkus.oidc.test;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.runtime.OidcUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantConfigResolver implements TenantConfigResolver {
    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext context, OidcRequestContext<OidcTenantConfig> requestContext) {
        if (context.request().path().endsWith("/tenant-config-resolver")) {
            OidcTenantConfig config = OidcTenantConfig.builder()
                    .tenantId("tenant-config-resolver")
                    .authServerUrl(getIssuerUrl() + "/realms/quarkus")
                    .clientId("quarkus-web-app")
                    .credentials("secret")
                    .applicationType(ApplicationType.WEB_APP)
                    .build();
            return Uni.createFrom().item(config);
        }
        context.remove(OidcUtils.TENANT_ID_ATTRIBUTE);
        if (context.request().path().endsWith("/null-tenant")) {
            return null;
        }
        return Uni.createFrom().nullItem();
    }

    private String getIssuerUrl() {
        return ConfigProvider.getConfig().getValue("keycloak.url", String.class);
    }
}
