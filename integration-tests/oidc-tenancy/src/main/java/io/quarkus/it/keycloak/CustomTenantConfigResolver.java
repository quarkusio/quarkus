package io.quarkus.it.keycloak;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.OidcTenantConfig.Roles.Source;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.common.runtime.OidcClientCommonConfig.Credentials;
import io.quarkus.oidc.common.runtime.OidcClientCommonConfig.Credentials.Secret.Method;
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
                if (context.normalizedPath().startsWith("/ws/tenant-annotation/bearer-step-up-auth")
                        || context.normalizedPath().startsWith("/tenant-ann-step-up-auth")) {
                    // use @Tenant annotation to resolve configuration
                    return null;
                }

                // Make sure this resolver is called only once during a given request
                if (context.get("dynamic_config_resolved") != null) {
                    throw new RuntimeException();
                }
                context.put("dynamic_config_resolved", "true");

                String path = context.request().path();
                String tenantId = path.split("/")[2];

                if ("tenant-d".equals(tenantId)) {
                    OidcTenantConfig config = new OidcTenantConfig();
                    config.setTenantId("tenant-c");
                    config.setAuthServerUrl(getIssuerUrl() + "/realms/quarkus-d");
                    config.setClientId("quarkus-app-d");
                    config.getCredentials().setSecret("secret");
                    config.getToken().setIssuer(getIssuerUrl() + "/realms/quarkus-d");
                    config.getAuthentication().setUserInfoRequired(true);
                    config.setAllowUserInfoCache(false);
                    return config;
                } else if ("tenant-oidc".equals(tenantId) || context.normalizedPath().startsWith("/step-up-auth")) {
                    OidcTenantConfig config = new OidcTenantConfig();
                    config.setTenantId("tenant-oidc");
                    String uri = context.request().absoluteURI();
                    // authServerUri points to the JAX-RS `OidcResource`, root path is `/oidc`
                    final String authServerUri;
                    if (path.contains("tenant-opaque")) {
                        if (path.endsWith("/tenant-opaque/tenant-oidc/api/user")) {
                            authServerUri = uri.replace("/tenant-opaque/tenant-oidc/api/user", "/oidc");
                        } else if (path.endsWith("/tenant-opaque/tenant-oidc/api/user-permission")) {
                            authServerUri = uri.replace("/tenant-opaque/tenant-oidc/api/user-permission", "/oidc");
                        } else {
                            authServerUri = uri.replace("/tenant-opaque/tenant-oidc/api/admin-permission", "/oidc");
                        }
                    } else {
                        if (path.contains("/step-up-auth")) {
                            authServerUri = uri.substring(0, uri.indexOf("/step-up-auth")) + "/oidc";
                        } else {
                            authServerUri = uri.replace("/tenant/tenant-oidc/api/user", "/oidc");
                        }
                    }
                    config.setAuthServerUrl(authServerUri);
                    config.setClientId("client");
                    config.setAllowTokenIntrospectionCache(false);
                    // auto-discovery in Quarkus is enabled but the OIDC server returns an empty document, set the required endpoints in the config
                    // try the path relative to the authServerUri
                    config.setJwksPath("jwks");
                    // try the absolute URI
                    config.setIntrospectionPath(authServerUri + "/introspect");
                    return config;
                } else if ("tenant-introspection-multiple-required-claims".equals(tenantId)) {
                    String uri = context.request().absoluteURI();
                    String authServerUri = uri.replace("/tenant-introspection/tenant-introspection-multiple-required-claims",
                            "/oidc");
                    return OidcTenantConfig
                            .authServerUrl(authServerUri)
                            .tenantId("tenant-introspection-multiple-required-claims")
                            .discoveryEnabled(false)
                            .clientId("client")
                            .introspectionPath(authServerUri + "/introspect")
                            .allowTokenIntrospectionCache(false)
                            .token().requiredClaims("required_claim", Set.of("1", "2")).end()
                            .build();
                } else if ("tenant-introspection-required-claims".equals(tenantId)) {

                    OidcTenantConfig config = new OidcTenantConfig();
                    config.setTenantId("tenant-introspection-required-claims");
                    config.token.setRequiredClaims(Map.of("required_claim", Set.of("1")));
                    String uri = context.request().absoluteURI();
                    String authServerUri = uri.replace("/tenant-introspection/tenant-introspection-required-claims",
                            "/oidc");
                    config.setAuthServerUrl(authServerUri);
                    config.setDiscoveryEnabled(false);
                    config.setClientId("client");
                    config.setIntrospectionPath(authServerUri + "/introspect");
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
                } else if ("tenant-oidc-introspection-only".equals(tenantId)) {
                    OidcTenantConfig config = new OidcTenantConfig();
                    config.setTenantId(tenantId);
                    String uri = context.request().absoluteURI();
                    String authServerUri = uri.replace("/tenant/" + tenantId + "/api/user", "/oidc");
                    config.setAuthServerUrl(authServerUri);
                    config.setDiscoveryEnabled(false);
                    config.authentication.setUserInfoRequired(true);
                    config.token.setSubjectRequired(true);
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
                    config.getToken().setRefreshTokenTimeSkew(Duration.ofSeconds(3));
                    config.setAuthServerUrl(getIssuerUrl() + "/realms/quarkus-webapp");
                    config.setClientId("quarkus-app-webapp");
                    config.getCredentials().getClientSecret().setValue(
                            "AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow");
                    config.getCredentials().getClientSecret().setMethod(Method.POST);

                    // Let Keycloak issue a login challenge but use the test token endpoint
                    String uri = context.request().absoluteURI();
                    String tokenUri = uri.replace("/tenant-refresh/tenant-web-app-refresh/api/user", "/oidc/token");
                    config.setTokenPath(tokenUri);
                    String jwksUri = uri.replace("/tenant-refresh/tenant-web-app-refresh/api/user", "/oidc/jwks");
                    config.setJwksPath(jwksUri);
                    String userInfoPath = uri.replace("/tenant-refresh/tenant-web-app-refresh/api/user", "/oidc/userinfo");
                    config.setUserInfoPath(userInfoPath);
                    config.getToken().setIssuer("any");
                    config.tokenStateManager.setSplitTokens(true);
                    config.tokenStateManager.setEncryptionRequired(false);
                    config.getAuthentication().setSessionAgeExtension(Duration.ofMinutes(1));
                    config.getAuthentication().setIdTokenRequired(false);
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
        return ConfigProvider.getConfig().getValue("keycloak.url", String.class);
    }
}
