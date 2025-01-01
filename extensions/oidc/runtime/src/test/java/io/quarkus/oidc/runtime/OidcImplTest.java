package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

public class OidcImplTest {

    @Test
    public void testCreateServiceApp() {
        final OidcImpl oidc = new OidcImpl(getEmptyConfig());
        oidc.createServiceApp("auth-server-url-1");
        final OidcTenantConfig defaultTenantConfig = oidc.getDefaultTenantConfig();
        assertEquals("auth-server-url-1", defaultTenantConfig.authServerUrl().get());
        assertEquals(OidcTenantConfig.ApplicationType.SERVICE, defaultTenantConfig.applicationType().get());
    }

    @Test
    public void testCreateWebApp() {
        final OidcImpl oidc = new OidcImpl(getEmptyConfig());
        oidc.createWebApp("auth-server-url-1", "client5", "secret2");
        final OidcTenantConfig defaultTenantConfig = oidc.getDefaultTenantConfig();
        assertEquals("auth-server-url-1", defaultTenantConfig.authServerUrl().get());
        assertEquals(OidcTenantConfig.ApplicationType.WEB_APP, defaultTenantConfig.applicationType().get());
        assertEquals("client5", defaultTenantConfig.clientId().get());
        assertEquals("secret2", defaultTenantConfig.credentials().secret().get());
    }

    private static OidcConfig getEmptyConfig() {
        return new OidcConfig() {
            @Override
            public Map<String, OidcTenantConfig> namedTenants() {
                return Map.of(OidcConfig.DEFAULT_TENANT_KEY, io.quarkus.oidc.OidcTenantConfig.builder().build());
            }

            @Override
            public TokenCache tokenCache() {
                return null;
            }

            @Override
            public boolean resolveTenantsWithIssuer() {
                return false;
            }
        };
    }
}
