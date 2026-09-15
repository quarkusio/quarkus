package io.quarkus.oidc.runtime;

import static io.quarkus.oidc.runtime.OidcTenantConfig.Token.DecryptionAlgorithm.A256GCMKW;
import static io.quarkus.oidc.runtime.OidcTenantConfig.Token.DecryptionAlgorithm.RSA_OAEP_256;
import static io.quarkus.oidc.runtime.TenantConfigContextImpl.verifyTokenDecryptionAlgorithm;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.runtime.OidcTenantConfig.Token.DecryptionAlgorithm;
import io.quarkus.runtime.configuration.ConfigurationException;

public class TokenDecryptionAlgorithmTest {

    @Test
    public void testAlgorithmIsNotConfigured() {
        verifyTokenDecryptionAlgorithm(config(null), true);
        verifyTokenDecryptionAlgorithm(config(null), false);
    }

    @Test
    public void testAsymmetricAlgorithmWithPrivateKey() {
        verifyTokenDecryptionAlgorithm(config(RSA_OAEP_256), true);
    }

    @Test
    public void testAsymmetricAlgorithmWithoutPrivateKey() {
        var ex = assertThrows(ConfigurationException.class,
                () -> verifyTokenDecryptionAlgorithm(config(RSA_OAEP_256), false));
        assertEquals("Tenant tenant-a requires the RSA_OAEP_256 token decryption algorithm"
                + " but no private decryption key is available", ex.getMessage());
    }

    @Test
    public void testSymmetricAlgorithmWithoutPrivateKey() {
        verifyTokenDecryptionAlgorithm(config(A256GCMKW), false);
    }

    @Test
    public void testSymmetricAlgorithmWithPrivateKey() {
        var ex = assertThrows(ConfigurationException.class,
                () -> verifyTokenDecryptionAlgorithm(config(A256GCMKW), true));
        assertEquals("Tenant tenant-a requires the A256GCMKW token decryption algorithm"
                + " but a private decryption key is configured", ex.getMessage());
    }

    private static OidcTenantConfig config(DecryptionAlgorithm decryptionAlgorithm) {
        return OidcTenantConfig.builder().tenantId("tenant-a")
                .token().decryptionAlgorithm(decryptionAlgorithm).end()
                .build();
    }
}
