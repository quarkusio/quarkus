package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    public void testClientSecretProviderRejected() {
        var config = cimdBuilder()
                .credentials().clientSecret().provider().key("vault-key").end().end().end()
                .build();
        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> TenantContextFactory.verifyClientIdMetadataConfiguration(config, "test"));
        assertTrue(ex.getMessage().contains("shared secret"));
    }

    @Test
    public void testJwtSecretProviderRejected() {
        var config = cimdBuilder()
                .credentials().jwt().secretProvider().key("vault-key").end().end().end()
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
    public void testMalformedUrlRejected() {
        OidcTenantConfig config = cimdBuilder().clientId("https://example.com/client metadata").build();
        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> TenantContextFactory.verifyClientIdMetadataConfiguration(config, "test"));
        assertTrue(ex.getMessage().contains("is not a valid URL"), ex.getMessage());
    }

    @Test
    public void testSchemeRelativeRedirectPathRejected() {
        OidcTenantConfig config = cimdBuilder()
                .authentication().redirectPath("//example.com/callback").end()
                .build();
        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> TenantContextFactory.verifyClientIdMetadataConfiguration(config, "test"));
        assertTrue(ex.getMessage().contains("redirect-path") && ex.getMessage().contains("must not start with '//'"),
                ex.getMessage());
    }

    @Test
    public void testMalformedRedirectPathRejected() {
        OidcTenantConfig config = cimdBuilder()
                .authentication().redirectPath("https://example.com/call back").end()
                .build();
        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> TenantContextFactory.verifyClientIdMetadataConfiguration(config, "test"));
        assertTrue(ex.getMessage().contains("redirect-path") && ex.getMessage().contains("is not a valid URI"),
                ex.getMessage());
    }

    @Test
    public void testSingleDotPathComponentRejected() {
        OidcTenantConfig config = cimdBuilder().clientId("https://example.com/./client-metadata").build();
        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> TenantContextFactory.verifyClientIdMetadataConfiguration(config, "test"));
        assertTrue(ex.getMessage().contains("single-dot or double-dot"), ex.getMessage());
    }

    @Test
    public void testDoubleDotPathComponentRejected() {
        OidcTenantConfig config = cimdBuilder().clientId("https://example.com/apps/../client-metadata").build();
        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> TenantContextFactory.verifyClientIdMetadataConfiguration(config, "test"));
        assertTrue(ex.getMessage().contains("single-dot or double-dot"), ex.getMessage());
    }

    @Test
    public void testEncodedDotPathComponentRejected() {
        OidcTenantConfig config = cimdBuilder().clientId("https://example.com/apps/%2E%2E/client-metadata").build();
        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> TenantContextFactory.verifyClientIdMetadataConfiguration(config, "test"));
        assertTrue(ex.getMessage().contains("single-dot or double-dot"), ex.getMessage());
    }

    @Test
    public void testUserInfoComponentRejected() {
        OidcTenantConfig config = cimdBuilder().clientId("https://alice@example.com/client-metadata").build();
        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> TenantContextFactory.verifyClientIdMetadataConfiguration(config, "test"));
        assertTrue(ex.getMessage().contains("userinfo component"), ex.getMessage());
    }

    @Test
    public void testQueryComponentRejected() {
        OidcTenantConfig config = cimdBuilder().clientId("https://example.com/client-metadata?version=1").build();
        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> TenantContextFactory.verifyClientIdMetadataConfiguration(config, "test"));
        assertTrue(ex.getMessage().contains("query component"), ex.getMessage());
    }

    @Test
    public void testFragmentComponentRejected() {
        OidcTenantConfig config = cimdBuilder().clientId("https://example.com/client-metadata#section").build();
        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> TenantContextFactory.verifyClientIdMetadataConfiguration(config, "test"));
        assertTrue(ex.getMessage().contains("fragment component"), ex.getMessage());
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
                .credentials().jwt().keyFile("private-key.pem").publicKeyFile("public-key.pem").end().end()
                .build();
        assertDoesNotThrow(
                () -> TenantContextFactory.verifyClientIdMetadataConfiguration(config, "test"));
    }

    @Test
    public void testPrivateKeyWithoutPublicKeyRejected() {
        var config = cimdBuilder()
                .credentials().jwt().keyFile("private-key.pem").end().end()
                .build();
        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> TenantContextFactory.verifyClientIdMetadataConfiguration(config, "test"));
        assertTrue(ex.getMessage().contains("A JWT private key is configured")
                && ex.getMessage().contains("no JWT public key is set"), ex.getMessage());
    }

    @Test
    public void testPublicKeyWithoutPrivateKeyRejected() {
        var config = cimdBuilder()
                .credentials().jwt().publicKeyFile("public-key.pem").end().end()
                .build();
        ConfigurationException ex = assertThrows(ConfigurationException.class,
                () -> TenantContextFactory.verifyClientIdMetadataConfiguration(config, "test"));
        assertTrue(ex.getMessage().contains("A JWT public key is configured")
                && ex.getMessage().contains("no JWT private key is set"), ex.getMessage());
    }

    @Test
    public void testHttpClientIdRejectedWhenForceHttpsScheme() {
        var config = OidcTenantConfig.builder()
                .clientId("http://example.com/client-metadata")
                .clientName("Test App")
                .applicationType(ApplicationType.WEB_APP)
                .authentication().redirectPath("/callback").end()
                .build();
        assertFalse(ClientIdMetadataHandler.isClientIdMetadataUrl(config));
    }

    @Test
    public void testHttpClientIdAcceptedWhenForceHttpsSchemeFalse() {
        var config = OidcTenantConfig.builder()
                .clientId("http://example.com/client-metadata")
                .clientName("Test App")
                .applicationType(ApplicationType.WEB_APP)
                .authentication().redirectPath("/callback").end()
                .clientIdMetadata().forceHttpsScheme(false).end()
                .build();
        assertTrue(ClientIdMetadataHandler.isClientIdMetadataUrl(config));
        assertDoesNotThrow(
                () -> TenantContextFactory.verifyClientIdMetadataConfiguration(config, "test"));
    }

    @Test
    public void testMetadataUnchanged() {
        assertFalse(ClientIdMetadataHandler.metadataChanged(cimdBuilder().build(), cimdBuilder().build()));
    }

    @Test
    public void testMetadataChangedWhenClientIdChanged() {
        OidcTenantConfig config = cimdBuilder().clientId("https://example.com/other-client-metadata").build();
        assertTrue(ClientIdMetadataHandler.metadataChanged(config, cimdBuilder().build()));
    }

    @Test
    public void testMetadataChangedWhenClientNameChanged() {
        OidcTenantConfig config = cimdBuilder().clientName("Other App").build();
        assertTrue(ClientIdMetadataHandler.metadataChanged(config, cimdBuilder().build()));
    }

    @Test
    public void testMetadataChangedWhenRedirectPathChanged() {
        OidcTenantConfig config = cimdBuilder()
                .authentication().redirectPath("/other-callback").end()
                .build();
        assertTrue(ClientIdMetadataHandler.metadataChanged(config, cimdBuilder().build()));
    }

    @Test
    public void testMetadataChangedWhenPublicKeyChanged() {
        OidcTenantConfig previousConfig = cimdBuilder()
                .credentials().jwt().publicKey("public-key").end().end()
                .build();
        OidcTenantConfig config = cimdBuilder()
                .credentials().jwt().publicKey("other-public-key").end().end()
                .build();
        assertTrue(ClientIdMetadataHandler.metadataChanged(config, previousConfig));
    }

    @Test
    public void testMetadataChangedWhenPublicKeyFileChanged() {
        OidcTenantConfig previousConfig = cimdBuilder()
                .credentials().jwt().publicKeyFile("public-key.pem").end().end()
                .build();
        OidcTenantConfig config = cimdBuilder()
                .credentials().jwt().publicKeyFile("other-public-key.pem").end().end()
                .build();
        assertTrue(ClientIdMetadataHandler.metadataChanged(config, previousConfig));
    }

    private static OidcTenantConfigBuilder cimdBuilder() {
        return OidcTenantConfig.builder()
                .clientId("https://example.com/client-metadata")
                .clientName("Test App")
                .applicationType(ApplicationType.WEB_APP)
                .authentication().redirectPath("/callback").end();
    }
}
