package io.quarkus.oidc.common.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig.Credentials;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfigBuilder.CredentialsBuilder;

public class OidcClientCommonConfigBuilderTest {

    @Test
    public void testCredentialsBuilderDefaultValues() {
        Credentials credentials = new CredentialsBuilder<>().build();
        assertNotNull(credentials);
        assertTrue(credentials.secret().isEmpty());
        var clientSecret = credentials.clientSecret();
        assertNotNull(clientSecret);
        assertTrue(clientSecret.value().isEmpty());
        assertTrue(clientSecret.method().isEmpty());
        var provider = clientSecret.provider();
        assertNotNull(provider);
        assertTrue(provider.key().isEmpty());
        assertTrue(provider.keyringName().isEmpty());
        assertTrue(provider.name().isEmpty());
        var jwt = credentials.jwt();
        assertNotNull(jwt);
        assertEquals(Credentials.Jwt.Source.CLIENT, jwt.source());
        assertTrue(jwt.secret().isEmpty());
        provider = jwt.secretProvider();
        assertNotNull(provider);
        assertTrue(provider.key().isEmpty());
        assertTrue(provider.keyringName().isEmpty());
        assertTrue(provider.name().isEmpty());
        assertTrue(jwt.key().isEmpty());
        assertTrue(jwt.keyFile().isEmpty());
        assertTrue(jwt.keyStoreFile().isEmpty());
        assertTrue(jwt.keyStorePassword().isEmpty());
        assertTrue(jwt.keyId().isEmpty());
        assertTrue(jwt.keyPassword().isEmpty());
        assertTrue(jwt.audience().isEmpty());
        assertTrue(jwt.tokenKeyId().isEmpty());
        assertTrue(jwt.issuer().isEmpty());
        assertTrue(jwt.subject().isEmpty());
        assertTrue(jwt.claims().isEmpty());
        assertTrue(jwt.signatureAlgorithm().isEmpty());
        assertEquals(10, jwt.lifespan());
        assertFalse(jwt.assertion());
        assertFalse(jwt.tokenPath().isPresent());
    }

}
