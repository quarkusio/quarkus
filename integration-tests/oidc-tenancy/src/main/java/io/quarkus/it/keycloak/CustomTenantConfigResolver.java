package io.quarkus.it.keycloak;

import java.time.Duration;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.OidcTenantConfig.Roles.Source;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.common.runtime.OidcCommonConfig.Credentials;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
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
                    // authServerUri points to the JAX-RS `OidcResource`, root path is `/oidc`
                    String authServerUri = path.contains("tenant-opaque")
                            ? uri.replace("/tenant-opaque/tenant-oidc/api/user", "/oidc")
                            : uri.replace("/tenant/tenant-oidc/api/user", "/oidc");
                    config.setAuthServerUrl(authServerUri);
                    config.setClientId("client");
                    config.setAllowTokenIntrospectionCache(false);
                    // auto-discovery in Quarkus is enabled but the OIDC server returns an empty document, set the required endpoints in the config
                    // try the path relative to the authServerUri
                    config.setJwksPath("jwks");
                    // try the absolute URI
                    config.setIntrospectionPath(authServerUri + "/introspect");

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
                } else if ("tenant-oidc-introspection-only".equals(tenantId)) {
                    OidcTenantConfig config = new OidcTenantConfig();
                    config.setTenantId(tenantId);
                    String uri = context.request().absoluteURI();
                    String authServerUri = uri.replace("/tenant/" + tenantId + "/api/user", "/oidc");
                    config.setAuthServerUrl(authServerUri);
                    config.setDiscoveryEnabled(false);
                    config.authentication.setUserInfoRequired(true);
                    config.setIntrospectionPath("introspect");
                    config.setUserInfoPath("userinfo");
                    config.setClientId("client-introspection-only");
                    config.setAllowTokenIntrospectionCache(false);
                    config.setAllowUserInfoCache(false);
                    Credentials creds = config.getCredentials();
                    creds.clientSecret.setMethod(Credentials.Secret.Method.POST_JWT);
                    creds.getJwt().setKeyFile("ecPrivateKey.pem");
                    creds.getJwt().setSignatureAlgorithm(SignatureAlgorithm.ES256.getAlgorithm());
                    return config;
                } else if ("tenant-oidc-introspection-only-cache".equals(tenantId)) {
                    OidcTenantConfig config = new OidcTenantConfig();
                    config.setTenantId(tenantId);
                    String uri = context.request().absoluteURI();
                    String authServerUri = uri.replace("/tenant/" + tenantId + "/api/user", "/oidc");
                    config.setAuthServerUrl(authServerUri);
                    config.authentication.setUserInfoRequired(true);
                    config.setUserInfoPath("userinfo");
                    config.setClientId("client-introspection-only-cache");
                    config.getIntrospectionCredentials().setName("bob");
                    config.getIntrospectionCredentials().setSecret("bob_secret");
                    config.getToken().setRequireJwtIntrospectionOnly(true);
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
                } else if ("tenant-web-app-refresh".equals(tenantId)) {
                    OidcTenantConfig config = new OidcTenantConfig();
                    config.setTenantId("tenant-web-app-refresh");
                    config.setApplicationType(ApplicationType.WEB_APP);
                    config.getToken().setRefreshExpired(true);
                    config.setAuthServerUrl(getIssuerUrl() + "/realms/quarkus-webapp");
                    config.setClientId("quarkus-app-webapp");
                    config.getCredentials().setSecret("secret");

                    // Let Keycloak issue a login challenge but use the test token endpoint
                    String uri = context.request().absoluteURI();
                    String tokenUri = uri.replace("/tenant-refresh/tenant-web-app-refresh/api/user", "/oidc/token");
                    config.setTokenPath(tokenUri);
                    String jwksUri = uri.replace("/tenant-refresh/tenant-web-app-refresh/api/user", "/oidc/jwks");
                    config.setJwksPath(jwksUri);
                    config.getToken().setIssuer("any");
                    config.tokenStateManager.setSplitTokens(true);
                    config.getAuthentication().setSessionAgeExtension(Duration.ofMinutes(1));
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
