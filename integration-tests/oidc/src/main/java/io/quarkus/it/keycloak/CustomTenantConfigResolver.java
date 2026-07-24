package io.quarkus.it.keycloak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantConfigResolver implements TenantConfigResolver {

    @Inject
    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String authServerUrl;

    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext context, OidcRequestContext<OidcTenantConfig> requestContext) {
        if (context.request().path().contains("tenant-cimd-dynamic")) {
            return Uni.createFrom().item(OidcTenantConfig.builder()
                    .tenantId("tenant-cimd-dynamic")
                    .authServerUrl(authServerUrl)
                    .clientId("https://host.testcontainers.internal:8444/client-id-metadata/tenant-cimd-dynamic")
                    .clientName("Tenant CIMD Dynamic")
                    .authentication().redirectPath("/tenant-cimd-dynamic").end()
                    .tlsConfigurationName("oidc-tls")
                    .applicationType(ApplicationType.WEB_APP)
                    .build());
        }
        return Uni.createFrom().nullItem();
    }
}
