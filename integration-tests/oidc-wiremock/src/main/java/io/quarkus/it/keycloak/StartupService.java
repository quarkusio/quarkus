package io.quarkus.it.keycloak;

import static io.quarkus.oidc.runtime.OidcUtils.DEFAULT_TENANT_ID;
import static io.quarkus.oidc.runtime.OidcUtils.TENANT_ID_ATTRIBUTE;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.OIDCException;
import io.quarkus.oidc.Tenant;
import io.quarkus.oidc.TenantIdentityProvider;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;
import io.smallrye.jwt.build.Jwt;

@Singleton
public class StartupService {

    private static final String ISSUER = "https://server.example.com";

    @Inject
    @Tenant("bearer")
    TenantIdentityProvider identityProviderBearer;

    @Inject
    @Tenant("bearer-role-claim-path")
    TenantIdentityProvider identityProviderBearerRoleClaimPath;

    private final Map<String, Map<String, Set<String>>> tenantToIdentityWithRole = new ConcurrentHashMap<>();

    void onStartup(@Observes StartupEvent event,
            @Tenant(DEFAULT_TENANT_ID) TenantIdentityProvider defaultTenantProvider,
            TenantIdentityProvider defaultTenantProviderDefaultQualifier) {
        assertDefaultTenantProviderInjection(defaultTenantProvider);
        assertDefaultTenantProviderInjection(defaultTenantProviderDefaultQualifier);
        String accessToken;
        String tenant;

        tenant = "bearer";
        accessToken = getAccessToken("alice", Set.of("user", "admin"), ISSUER);
        acquireSecurityIdentity(accessToken, tenant, identityProviderBearer);

        boolean wrongIssuerFailed = false;
        try {
            accessToken = getAccessToken("admin", Set.of("user", "admin"), "https://wrong-server.example.com");
            acquireSecurityIdentity(accessToken, tenant, identityProviderBearer);
        } catch (AuthenticationFailedException ignored) {
            wrongIssuerFailed = true;
        }
        if (!wrongIssuerFailed) {
            throw new IllegalStateException("User 'admin' shouldn't be able to acquire identity with wrong issuer");
        }

        tenant = "bearer-role-claim-path";

        // first prove this is indeed bearer-role-claim-path tenant
        accessToken = getAccessToken("admin", Set.of("admin"), ISSUER);
        acquireSecurityIdentity(accessToken, tenant, identityProviderBearerRoleClaimPath);
        var roles = tenantToIdentityWithRole.get(tenant).get("admin");
        if (!roles.isEmpty()) {
            throw new IllegalStateException(
                    "User 'admin' should have empty roles because wrong role path was used, but got: " + roles);
        }

        accessToken = getAccessTokenWithCustomRolePath("admin", Set.of("admin"));
        acquireSecurityIdentity(accessToken, tenant, identityProviderBearerRoleClaimPath);
    }

    private void assertDefaultTenantProviderInjection(TenantIdentityProvider defaultTenantProvider) {
        String tenant = DEFAULT_TENANT_ID;
        String accessToken = getAccessToken("bob", Set.of("user", "admin"), ISSUER);
        boolean oidcServerNotAvailable = false;
        try {
            acquireSecurityIdentity(accessToken, tenant, defaultTenantProvider);
        } catch (OIDCException exception) {
            if (exception.getMessage().contains("OIDC Server is not available")) {
                oidcServerNotAvailable = true;
            }
        }
        if (!oidcServerNotAvailable) {
            throw new IllegalStateException("Default OIDC tenant should have incorrect auth server URL");
        }
    }

    private void acquireSecurityIdentity(String accessToken, String tenant, TenantIdentityProvider identityProvider) {
        SecurityIdentity identity = identityProvider.authenticate(new AccessTokenCredential(accessToken)).await()
                .indefinitely();

        // validate identity
        if (!identity.getAttribute(TENANT_ID_ATTRIBUTE).equals(tenant)) {
            throw new IllegalStateException(
                    "SecurityIdentity should contain '" + tenant + "' as '" + TENANT_ID_ATTRIBUTE + "' attribute");
        }
        String issuer = ((DefaultJWTCallerPrincipal) identity.getPrincipal()).getIssuer();
        if (!ISSUER.equals(issuer)) {
            throw new IllegalStateException("Expected issuer '" + ISSUER + "' but got '" + issuer + "'");
        }

        Set<String> roles = identity.getRoles();
        String username = identity.getPrincipal().getName();
        tenantToIdentityWithRole.computeIfAbsent(tenant, s -> new HashMap<>()).put(username, roles);
    }

    Map<String, Map<String, Set<String>>> getTenantToIdentityWithRole() {
        return tenantToIdentityWithRole;
    }

    private static String getAccessToken(String userName, Set<String> groups, String issuer) {
        return getAccessToken(userName, groups, SignatureAlgorithm.RS256, issuer);
    }

    private static String getAccessToken(String userName, Set<String> groups, SignatureAlgorithm alg, String issuer) {
        return Jwt.preferredUserName(userName)
                .groups(groups)
                .issuer(issuer)
                .audience("https://service.example.com")
                .jws().algorithm(alg)
                .sign();
    }

    private static String getAccessTokenWithCustomRolePath(String userName, Set<String> groups) {
        return Jwt.preferredUserName(userName)
                .claim("https://roles.example.com", groups)
                .issuer("https://server.example.com")
                .audience("https://service.example.com")
                .sign();
    }
}
