package io.quarkus.it.keycloak;

import java.time.Duration;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig.Credentials.Secret.Method;
import io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.runtime.OidcTenantConfig.Roles.Source;
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
                    return OidcTenantConfig.authServerUrl(getIssuerUrl() + "/realms/quarkus-d")
                            .tenantId("tenant-c")
                            .clientId("quarkus-app-d")
                            .credentials("secret")
                            .token().issuer(getIssuerUrl() + "/realms/quarkus-d").end()
                            .authentication().userInfoRequired(true).end()
                            .allowUserInfoCache(false)
                            .build();
                } else if ("tenant-oidc".equals(tenantId) || context.normalizedPath().startsWith("/step-up-auth")) {
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
                    // auto-discovery in Quarkus is enabled but the OIDC server returns an empty document, set the required endpoints in the config
                    return OidcTenantConfig.authServerUrl(authServerUri)
                            .tenantId("tenant-oidc")
                            .clientId("client")
                            .allowTokenIntrospectionCache(false)
                            // try the path relative to the authServerUri
                            .jwksPath("jwks")
                            // try the absolute URI
                            .introspectionPath(authServerUri + "/introspect")
                            .build();
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
                    String uri = context.request().absoluteURI();
                    String authServerUri = uri.replace("/tenant-introspection/tenant-introspection-required-claims",
                            "/oidc");
                    return OidcTenantConfig.builder()
                            .tenantId("tenant-introspection-required-claims")
                            .token().requiredClaims("required_claim", Set.of("1")).end()
                            .authServerUrl(authServerUri)
                            .discoveryEnabled(false)
                            .clientId("client")
                            .introspectionPath(authServerUri + "/introspect")
                            .allowTokenIntrospectionCache(false)
                            .build();
                } else if ("tenant-oidc-no-discovery".equals(tenantId)) {
                    String uri = context.request().absoluteURI();
                    String authServerUri = uri.replace("/tenant/tenant-oidc-no-discovery/api/user", "/oidc");
                    return OidcTenantConfig.authServerUrl(authServerUri)
                            .tenantId("tenant-oidc-no-discovery")
                            .discoveryEnabled(false)
                            .jwksPath("jwks")
                            .clientId("client")
                            .build();
                } else if ("tenant-oidc-no-introspection".equals(tenantId)) {
                    String uri = context.request().absoluteURI();
                    String authServerUri = uri.replace("/tenant/tenant-oidc-no-introspection/api/user", "/oidc");
                    return OidcTenantConfig.builder()
                            .tenantId("tenant-oidc-no-introspection")
                            .authServerUrl(authServerUri)
                            .token().allowJwtIntrospection(false).end()
                            .clientId("client")
                            .build();
                } else if ("tenant-oidc-introspection-only".equals(tenantId)) {
                    String uri = context.request().absoluteURI();
                    String authServerUri = uri.replace("/tenant/" + tenantId + "/api/user", "/oidc");
                    return OidcTenantConfig.authServerUrl(authServerUri)
                            .tenantId(tenantId)
                            .discoveryEnabled(false)
                            .authentication().userInfoRequired(true).end()
                            .token().subjectRequired(true).end()
                            .introspectionPath("introspect")
                            .userInfoPath("userinfo")
                            .clientId("client-introspection-only")
                            .allowTokenIntrospectionCache(false)
                            .allowUserInfoCache(false)
                            .credentials()
                            .clientSecret().method(Method.POST_JWT).end()
                            .jwt().keyFile("ecPrivateKey.pem")
                            .signatureAlgorithm(SignatureAlgorithm.ES256.getAlgorithm())
                            .endCredentials()
                            .build();
                } else if ("tenant-oidc-introspection-only-cache".equals(tenantId)) {
                    String uri = context.request().absoluteURI();
                    String authServerUri = uri.replace("/tenant/" + tenantId + "/api/user", "/oidc");
                    return OidcTenantConfig.authServerUrl(authServerUri)
                            .tenantId(tenantId)
                            .authentication().userInfoRequired(true).end()
                            .userInfoPath("userinfo")
                            .clientId("client-introspection-only-cache")
                            .introspectionCredentials().name("bob").secret("bob_secret").end()
                            .token().requireJwtIntrospectionOnly(true).end()
                            .build();
                } else if ("tenant-oidc-no-opaque-token".equals(tenantId)) {
                    String uri = context.request().absoluteURI();
                    String authServerUri = uri.replace("/tenant-opaque/tenant-oidc-no-opaque-token/api/user", "/oidc");
                    return OidcTenantConfig.authServerUrl(authServerUri)
                            .tenantId("tenant-oidc-no-opaque-token")
                            .token().allowOpaqueTokenIntrospection(false).end()
                            .clientId("client")
                            .build();
                } else if ("tenant-web-app-refresh".equals(tenantId)) {
                    // Let Keycloak issue a login challenge but use the test token endpoint
                    String uri = context.request().absoluteURI();
                    String tokenUri = uri.replace("/tenant-refresh/tenant-web-app-refresh/api/user",
                            "/oidc/token");
                    String jwksUri = uri.replace("/tenant-refresh/tenant-web-app-refresh/api/user",
                            "/oidc/jwks");
                    String userInfoPath = uri.replace("/tenant-refresh/tenant-web-app-refresh/api/user",
                            "/oidc/userinfo");
                    return OidcTenantConfig.builder()
                            .tenantId("tenant-web-app-refresh")
                            .applicationType(ApplicationType.WEB_APP)
                            .authServerUrl(getIssuerUrl() + "/realms/quarkus-webapp")
                            .clientId("quarkus-app-webapp")
                            .credentials()
                            .clientSecret()
                            .value("AyM1SysPpbyDfgZld3umj1qzKObwVMkoqQ-EstJQLr_T-1qS0gZH75aKtMN3Yj0iPS4hcgUuTwjAzZr1Z9CAow")
                            .method(Method.POST)
                            .end()
                            .end()
                            .tokenPath(tokenUri)
                            .jwksPath(jwksUri)
                            .userInfoPath(userInfoPath)
                            .token()
                            .refreshExpired(true)
                            .refreshTokenTimeSkew(Duration.ofSeconds(3))
                            .issuer("any")
                            .end()
                            .tokenStateManager().splitTokens(true).encryptionRequired(false).end()
                            .authentication()
                            .sessionAgeExtension(Duration.ofMinutes(1))
                            .idTokenRequired(false)
                            .end()
                            .build();
                } else if ("tenant-web-app-dynamic".equals(tenantId)) {
                    return OidcTenantConfig.authServerUrl(getIssuerUrl() + "/realms/quarkus-webapp")
                            .tenantId("tenant-web-app-dynamic")
                            .clientId("quarkus-app-webapp")
                            .credentials("secret")
                            .authentication().userInfoRequired(true).end()
                            .roles().source(Source.userinfo).end()
                            .allowUserInfoCache(false)
                            .applicationType(ApplicationType.WEB_APP)
                            .build();
                }
                return null;
            }
        });
    }

    private String getIssuerUrl() {
        return ConfigProvider.getConfig().getValue("keycloak.url", String.class);
    }
}
