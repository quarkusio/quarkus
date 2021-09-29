package io.quarkus.it.keycloak;

import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.OidcTenantConfig.Roles.Source;
import io.quarkus.oidc.TenantConfigResolver;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class CustomTenantConfigResolver implements TenantConfigResolver {

    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext context, OidcRequestContext<OidcTenantConfig> requestContext) {
        return requestContext.runBlocking(new Supplier<OidcTenantConfig>() {
            @Override
            public OidcTenantConfig get() {

                // Make sure this resolver is called only once during a given request
                if (context.get("dynamic_config_resolved") != null) {
                    throw new RuntimeException();
                }
                context.put("dynamic_config_resolved", "true");

                String path = context.request().path();
                String tenantId = path.split("/")[2];
                if ("tenant-d".equals(tenantId)) {
                    OidcTenantConfig config = new OidcTenantConfig();
                    config.setTenantId("tenant-d");
                    config.setAuthServerUrl(getIssuerUrl() + "/realms/quarkus-d");
                    config.setClientId("quarkus-d");
                    config.getCredentials().setSecret("secret");
                    config.getToken().setIssuer(getIssuerUrl() + "/realms/quarkus-d");
                    config.getAuthentication().setUserInfoRequired(true);
                    config.setAllowUserInfoCache(false);
                    return config;
                } else if ("tenant-oidc".equals(tenantId)) {
                    OidcTenantConfig config = new OidcTenantConfig();
                    config.setTenantId("tenant-oidc");
                    String uri = context.request().absoluteURI();
                    String authServerUri = path.contains("tenant-opaque")
                            ? uri.replace("/tenant-opaque/tenant-oidc/api/user", "/oidc")
                            : uri.replace("/tenant/tenant-oidc/api/user", "/oidc");
                    config.setAuthServerUrl(authServerUri);
                    config.setClientId("client");
                    config.setAllowTokenIntrospectionCache(false);
                    return config;
                } else if ("tenant-oidc-no-discovery".equals(tenantId)) {
                    OidcTenantConfig config = new OidcTenantConfig();
                    config.setTenantId("tenant-oidc-no-discovery");
                    String uri = context.request().absoluteURI();
                    String authServerUri = uri.replace("/tenant/tenant-oidc-no-discovery/api/user", "/oidc");
                    config.setAuthServerUrl(authServerUri);
                    config.setDiscoveryEnabled(false);
                    config.setJwksPath("jwks");
                    config.setClientId("client");
                    return config;
                } else if ("tenant-oidc-no-introspection".equals(tenantId)) {
                    OidcTenantConfig config = new OidcTenantConfig();
                    config.setTenantId("tenant-oidc-no-introspection");
                    String uri = context.request().absoluteURI();
                    String authServerUri = uri.replace("/tenant/tenant-oidc-no-introspection/api/user", "/oidc");
                    config.setAuthServerUrl(authServerUri);
                    config.token.setAllowJwtIntrospection(false);
                    config.setClientId("client");
                    return config;
                } else if ("tenant-oidc-introspection-only".equals(tenantId)
                        || "tenant-oidc-introspection-only-cache".equals(tenantId)) {
                    OidcTenantConfig config = new OidcTenantConfig();
                    config.setTenantId(tenantId);
                    String uri = context.request().absoluteURI();
                    String authServerUri = uri.replace("/tenant/" + tenantId + "/api/user", "/oidc");
                    config.setAuthServerUrl(authServerUri);
                    config.setDiscoveryEnabled(false);
                    config.authentication.setUserInfoRequired(true);
                    if ("tenant-oidc-introspection-only".equals(tenantId)) {
                        config.setAllowTokenIntrospectionCache(false);
                        config.setAllowUserInfoCache(false);
                    }
                    config.setIntrospectionPath("introspect");
                    config.setUserInfoPath("userinfo");
                    config.setClientId("client");
                    return config;
                } else if ("tenant-oidc-no-opaque-token".equals(tenantId)) {
                    OidcTenantConfig config = new OidcTenantConfig();
                    config.setTenantId("tenant-oidc-no-opaque-token");
                    String uri = context.request().absoluteURI();
                    String authServerUri = uri.replace("/tenant-opaque/tenant-oidc-no-opaque-token/api/user", "/oidc");
                    config.setAuthServerUrl(authServerUri);
                    config.token.setAllowOpaqueTokenIntrospection(false);
                    config.setClientId("client");
                    return config;
                } else if ("tenant-web-app-dynamic".equals(tenantId)) {
                    OidcTenantConfig config = new OidcTenantConfig();
                    config.setTenantId("tenant-web-app-dynamic");
                    config.setAuthServerUrl(getIssuerUrl() + "/realms/quarkus-webapp");
                    config.setClientId("quarkus-app-webapp");
                    config.getCredentials().setSecret("secret");
                    config.getAuthentication().setUserInfoRequired(true);
                    config.getRoles().setSource(Source.userinfo);
                    config.setAllowUserInfoCache(false);
                    config.setApplicationType(ApplicationType.WEB_APP);
                    return config;
                }
                return null;
            }
        });
    }

    private String getIssuerUrl() {
        return System.getProperty("keycloak.url", "http://localhost:8180/auth");
    }
}
