package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.common.runtime.OidcCommonUtils;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig.Credentials;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig.Credentials.Secret.Method;
import io.quarkus.runtime.configuration.ConfigurationException;

/**
 * The client authentication method configured with {@code client-secret.method} applies to a secret configured with
 * {@code credentials.secret} as well as to one configured with {@code credentials.client-secret.value}.
 */
public class OidcClientSecretMethodTest {

    @Test
    public void testSecretDefaultsToBasic() {
        Credentials credentials = OidcTenantConfig.builder().credentials().secret("s").end().build().credentials();
        assertTrue(OidcCommonUtils.isClientSecretBasicAuthRequired(credentials));
        assertFalse(OidcCommonUtils.isClientSecretPostAuthRequired(credentials));
    }

    @Test
    public void testSecretWithPostMethod() {
        Credentials credentials = OidcTenantConfig.builder().credentials().secret("s")
                .clientSecret().method(Method.POST).end().end().build().credentials();
        assertFalse(OidcCommonUtils.isClientSecretBasicAuthRequired(credentials));
        assertTrue(OidcCommonUtils.isClientSecretPostAuthRequired(credentials));
    }

    @Test
    public void testSecretWithBasicMethod() {
        Credentials credentials = OidcTenantConfig.builder().credentials().secret("s")
                .clientSecret().method(Method.BASIC).end().end().build().credentials();
        assertTrue(OidcCommonUtils.isClientSecretBasicAuthRequired(credentials));
        assertFalse(OidcCommonUtils.isClientSecretPostAuthRequired(credentials));
    }

    @Test
    public void testClientSecretValueWithPostMethod() {
        Credentials credentials = OidcTenantConfig.builder().credentials()
                .clientSecret().value("s").method(Method.POST).end().end().build().credentials();
        assertFalse(OidcCommonUtils.isClientSecretBasicAuthRequired(credentials));
        assertTrue(OidcCommonUtils.isClientSecretPostAuthRequired(credentials));
    }

    @Test
    public void testSecretWithPostMethodCannotBeSentToAllEndpoints() {
        Credentials credentials = OidcTenantConfig.builder().credentials().secret("s").forAllEndpoints()
                .clientSecret().method(Method.POST).end().end().build().credentials();
        assertThrows(ConfigurationException.class, () -> OidcCommonUtils.validateCredentialsForAllEndpoints(credentials));
    }
}
