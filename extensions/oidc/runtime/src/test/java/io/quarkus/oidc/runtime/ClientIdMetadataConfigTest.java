package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.OidcTenantConfigBuilder;
import io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType;
import io.quarkus.runtime.configuration.ConfigurationException;

public class ClientIdMetadataConfigTest {

    @Test
    public void testSecretBasicRejected() {
        var config = cimdBuilder()
                .credentials().secret("my-secret").end()
                .build();
        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> TenantContextFactory.verifyClientIdMetadataConfiguration(config, "test"));
        assertTrue(ex.getMessage().contains("shared secret"));
    }

    @Test
    public void testClientSecretPostRejected() {
        var config = cimdBuilder()
                .credentials().clientSecret().value("my-secret").end().end()
                .build();
        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> TenantContextFactory.verifyClientIdMetadataConfiguration(config, "test"));
        assertTrue(ex.getMessage().contains("shared secret"));
    }

    @Test
    public void testJwtSecretRejected() {
        var config = cimdBuilder()
                .credentials().jwt().secret("my-jwt-secret").end().end()
                .build();
        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> TenantContextFactory.verifyClientIdMetadataConfiguration(config, "test"));
        assertTrue(ex.getMessage().contains("shared secret"));
    }

    @Test
    public void testServiceAppRejected() {
        var config = OidcTenantConfig.builder()
                .clientId("https://example.com/client-metadata")
                .clientName("Test App")
                .applicationType(ApplicationType.SERVICE)
                .authentication().redirectPath("/callback").end()
                .build();
        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> TenantContextFactory.verifyClientIdMetadataConfiguration(config, "test"));
        assertTrue(ex.getMessage().contains("service"));
    }

    @Test
    public void testMissingClientNameRejected() {
        var config = OidcTenantConfig.builder()
                .clientId("https://example.com/client-metadata")
                .applicationType(ApplicationType.WEB_APP)
                .authentication().redirectPath("/callback").end()
                .build();
        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> TenantContextFactory.verifyClientIdMetadataConfiguration(config, "test"));
        assertTrue(ex.getMessage().contains("client-name"));
    }

    @Test
    public void testMissingRedirectPathRejected() {
        var config = OidcTenantConfig.builder()
                .clientId("https://example.com/client-metadata")
                .clientName("Test App")
                .applicationType(ApplicationType.WEB_APP)
                .build();
        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> TenantContextFactory.verifyClientIdMetadataConfiguration(config, "test"));
        assertTrue(ex.getMessage().contains("redirect-path"));
    }

    @Test
    public void testNoPathComponentRejected() {
        var config = OidcTenantConfig.builder()
                .clientId("https://example.com/")
                .clientName("Test App")
                .applicationType(ApplicationType.WEB_APP)
                .authentication().redirectPath("/callback").end()
                .build();
        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> TenantContextFactory.verifyClientIdMetadataConfiguration(config, "test"));
        assertTrue(ex.getMessage().contains("path component"));
    }

    @Test
    public void testPublicClientAccepted() {
        var config = cimdBuilder().build();
        assertDoesNotThrow(
                () -> TenantContextFactory.verifyClientIdMetadataConfiguration(config, "test"));
    }

    @Test
    public void testPrivateKeyJwtAccepted() {
        var config = cimdBuilder()
                .credentials().jwt().keyFile("private-key.pem").end().end()
                .build();
        assertDoesNotThrow(
                () -> TenantContextFactory.verifyClientIdMetadataConfiguration(config, "test"));
    }

    private static OidcTenantConfigBuilder cimdBuilder() {
        return OidcTenantConfig.builder()
                .clientId("https://example.com/client-metadata")
                .clientName("Test App")
                .applicationType(ApplicationType.WEB_APP)
                .authentication().redirectPath("/callback").end();
    }
}
