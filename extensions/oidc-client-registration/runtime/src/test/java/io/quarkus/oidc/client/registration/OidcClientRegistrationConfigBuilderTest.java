package io.quarkus.oidc.client.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.client.registration.OidcClientRegistrationConfigBuilder.MetadataBuilder;

public class OidcClientRegistrationConfigBuilderTest {

    @Test
    public void testDefaultValues() {
        var config = OidcClientRegistrationConfig.builder().build();
        testDefaultValues(config);
        config = new OidcClientRegistrationConfigBuilder().build();
        testDefaultValues(config);
    }

    private static void testDefaultValues(OidcClientRegistrationConfig config) {
        // OidcClientRegistrationConfig methods
        assertTrue(config.id().isEmpty());
        assertTrue(config.registrationEnabled());
        assertTrue(config.registerEarly());
        assertTrue(config.initialToken().isEmpty());
        assertNotNull(config.metadata());
        assertTrue(config.metadata().clientName().isEmpty());
        assertTrue(config.metadata().redirectUri().isEmpty());
        assertTrue(config.metadata().postLogoutUri().isEmpty());
        assertTrue(config.metadata().extraProps().isEmpty());

        // OidcCommonConfig methods
        assertTrue(config.authServerUrl().isEmpty());
        assertTrue(config.discoveryEnabled().isEmpty());
        assertTrue(config.registrationPath().isEmpty());
        assertTrue(config.connectionDelay().isEmpty());
        assertEquals(3, config.connectionRetryCount());
        assertEquals(10, config.connectionTimeout().getSeconds());
        assertFalse(config.useBlockingDnsLookup());
        assertTrue(config.maxPoolSize().isEmpty());
        assertTrue(config.followRedirects());
        assertNotNull(config.proxy());
        assertTrue(config.proxy().host().isEmpty());
        assertEquals(80, config.proxy().port());
        assertTrue(config.proxy().username().isEmpty());
        assertTrue(config.proxy().password().isEmpty());
        assertNotNull(config.tls());
        assertTrue(config.tls().tlsConfigurationName().isEmpty());
        assertTrue(config.tls().verification().isEmpty());
        assertTrue(config.tls().keyStoreFile().isEmpty());
        assertTrue(config.tls().keyStoreFileType().isEmpty());
        assertTrue(config.tls().keyStoreProvider().isEmpty());
        assertTrue(config.tls().keyStorePassword().isEmpty());
        assertTrue(config.tls().keyStoreKeyAlias().isEmpty());
        assertTrue(config.tls().keyStoreKeyPassword().isEmpty());
        assertTrue(config.tls().trustStoreFile().isEmpty());
        assertTrue(config.tls().trustStorePassword().isEmpty());
        assertTrue(config.tls().trustStoreCertAlias().isEmpty());
        assertTrue(config.tls().trustStoreFileType().isEmpty());
        assertTrue(config.tls().trustStoreProvider().isEmpty());
    }

    @Test
    public void testSetEveryProperty() {
        var config = OidcClientRegistrationConfig.builder()
                // OidcClientRegistrationConfig methods
                .id("pink")
                .registrationEnabled(false)
                .registerEarly(false)
                .initialToken("floyd")
                .metadata()
                .clientName("another")
                .redirectUri("brick")
                .postLogoutUri("in")
                .extraProps(Map.of("the", "wall"))
                .extraProperty("hey", "teacher")
                .end()
                // OidcCommonConfig methods
                .authServerUrl("we")
                .discoveryEnabled(false)
                .registrationPath("don't")
                .connectionDelay(Duration.ofSeconds(656))
                .connectionRetryCount(565)
                .connectionTimeout(Duration.ofSeconds(673))
                .useBlockingDnsLookup(true)
                .maxPoolSize(376)
                .followRedirects(false)
                .proxy("need", 55, "no", "education")
                .tlsConfigurationName("Teacher!")
                .build();

        // OidcClientRegistrationConfig methods
        assertEquals("pink", config.id().orElse(null));
        assertFalse(config.registrationEnabled());
        assertFalse(config.registerEarly());
        assertEquals("floyd", config.initialToken().orElse(null));
        assertNotNull(config.metadata());
        assertEquals("another", config.metadata().clientName().orElse(null));
        assertEquals("brick", config.metadata().redirectUri().orElse(null));
        assertEquals("in", config.metadata().postLogoutUri().orElse(null));
        assertEquals(2, config.metadata().extraProps().size());
        assertEquals("wall", config.metadata().extraProps().get("the"));
        assertEquals("teacher", config.metadata().extraProps().get("hey"));

        // OidcCommonConfig methods
        assertEquals("we", config.authServerUrl().orElse(null));
        assertFalse(config.discoveryEnabled().orElse(false));
        assertEquals("don't", config.registrationPath().orElse(null));
        assertEquals(656, config.connectionDelay().map(Duration::getSeconds).orElse(null));
        assertEquals(565, config.connectionRetryCount());
        assertEquals(673, config.connectionTimeout().getSeconds());
        assertTrue(config.useBlockingDnsLookup());
        assertEquals(376, config.maxPoolSize().orElse(0));
        assertFalse(config.followRedirects());
        assertNotNull(config.proxy());
        assertEquals("need", config.proxy().host().orElse(null));
        assertEquals(55, config.proxy().port());
        assertEquals("no", config.proxy().username().orElse(null));
        assertEquals("education", config.proxy().password().orElse(null));
        assertNotNull(config.tls());
        assertEquals("Teacher!", config.tls().tlsConfigurationName().orElse(null));
        assertTrue(config.tls().verification().isEmpty());
        assertTrue(config.tls().keyStoreFile().isEmpty());
        assertTrue(config.tls().keyStoreFileType().isEmpty());
        assertTrue(config.tls().keyStoreProvider().isEmpty());
        assertTrue(config.tls().keyStorePassword().isEmpty());
        assertTrue(config.tls().keyStoreKeyAlias().isEmpty());
        assertTrue(config.tls().keyStoreKeyPassword().isEmpty());
        assertTrue(config.tls().trustStoreFile().isEmpty());
        assertTrue(config.tls().trustStorePassword().isEmpty());
        assertTrue(config.tls().trustStoreCertAlias().isEmpty());
        assertTrue(config.tls().trustStoreFileType().isEmpty());
        assertTrue(config.tls().trustStoreProvider().isEmpty());
    }

    @Test
    public void testCopyProxyProperties() {
        var previousConfig = OidcClientRegistrationConfig.builder()
                .proxy("need", 55, "no", "education")
                .build();
        var newConfig = OidcClientRegistrationConfig.builder(previousConfig)
                .proxy("fast-car", 22)
                .build();

        assertNotNull(previousConfig.proxy());
        assertEquals("fast-car", newConfig.proxy().host().orElse(null));
        assertEquals(22, newConfig.proxy().port());
        assertEquals("no", newConfig.proxy().username().orElse(null));
        assertEquals("education", newConfig.proxy().password().orElse(null));
    }

    @Test
    public void testCopyClientRegistrationConfigProperties() {
        var previousConfigBuilder = OidcClientRegistrationConfig.builder();
        var previousConfig = new MetadataBuilder(previousConfigBuilder)
                .clientName("another")
                .redirectUri("brick")
                .postLogoutUri("in")
                .extraProps(Map.of("the", "wall"))
                .extraProperty("hey", "teacher")
                .end()
                .id("pink")
                .registrationEnabled(false)
                .registerEarly(false)
                .initialToken("floyd")
                .build();
        assertNotNull(previousConfig.metadata());
        assertEquals("another", previousConfig.metadata().clientName().orElse(null));
        assertEquals("brick", previousConfig.metadata().redirectUri().orElse(null));
        assertEquals("in", previousConfig.metadata().postLogoutUri().orElse(null));
        assertEquals(2, previousConfig.metadata().extraProps().size());
        assertEquals("wall", previousConfig.metadata().extraProps().get("the"));
        assertEquals("teacher", previousConfig.metadata().extraProps().get("hey"));

        var metadata = new MetadataBuilder()
                .clientName("place")
                .postLogoutUri("is")
                .extraProperty("better", "starting")
                .build();
        var newConfig = OidcClientRegistrationConfig.builder(previousConfig)
                .id("any")
                .registerEarly(true)
                .metadata(metadata)
                .build();

        assertEquals("any", newConfig.id().orElse(null));
        assertFalse(newConfig.registrationEnabled());
        assertTrue(newConfig.registerEarly());
        assertEquals("floyd", newConfig.initialToken().orElse(null));

        assertNotNull(newConfig.metadata());
        assertEquals("place", newConfig.metadata().clientName().orElse(null));
        assertTrue(newConfig.metadata().redirectUri().isEmpty());
        assertEquals("is", newConfig.metadata().postLogoutUri().orElse(null));
        assertEquals(1, newConfig.metadata().extraProps().size());
        assertEquals("starting", newConfig.metadata().extraProps().get("better"));
    }

    @Test
    public void testCopyOidcCommonConfigProperties() {
        var previousConfig = OidcClientRegistrationConfig.builder()
                .authServerUrl("we")
                .discoveryEnabled(false)
                .registrationPath("don't")
                .connectionDelay(Duration.ofSeconds(656))
                .connectionRetryCount(565)
                .connectionTimeout(Duration.ofSeconds(673))
                .useBlockingDnsLookup(true)
                .maxPoolSize(376)
                .followRedirects(false)
                .proxy("need", 55, "no", "education")
                .tlsConfigurationName("Teacher!")
                .build();
        var newConfig = OidcClientRegistrationConfig.builder(previousConfig)
                .discoveryEnabled(true)
                .connectionDelay(Duration.ofSeconds(753))
                .connectionTimeout(Duration.ofSeconds(357))
                .maxPoolSize(1988)
                .proxy("cross", 44, "the", "boarder")
                .build();

        assertEquals("we", newConfig.authServerUrl().orElse(null));
        assertTrue(newConfig.discoveryEnabled().orElse(false));
        assertEquals("don't", newConfig.registrationPath().orElse(null));
        assertEquals(753, newConfig.connectionDelay().map(Duration::getSeconds).orElse(null));
        assertEquals(565, newConfig.connectionRetryCount());
        assertEquals(357, newConfig.connectionTimeout().getSeconds());
        assertTrue(newConfig.useBlockingDnsLookup());
        assertEquals(1988, newConfig.maxPoolSize().orElse(0));
        assertFalse(newConfig.followRedirects());
        assertNotNull(newConfig.proxy());
        assertEquals("cross", newConfig.proxy().host().orElse(null));
        assertEquals(44, newConfig.proxy().port());
        assertEquals("the", newConfig.proxy().username().orElse(null));
        assertEquals("boarder", newConfig.proxy().password().orElse(null));
        assertNotNull(newConfig.tls());
        assertEquals("Teacher!", newConfig.tls().tlsConfigurationName().orElse(null));
        assertTrue(newConfig.tls().verification().isEmpty());
        assertTrue(newConfig.tls().keyStoreFile().isEmpty());
        assertTrue(newConfig.tls().keyStoreFileType().isEmpty());
        assertTrue(newConfig.tls().keyStoreProvider().isEmpty());
        assertTrue(newConfig.tls().keyStorePassword().isEmpty());
        assertTrue(newConfig.tls().keyStoreKeyAlias().isEmpty());
        assertTrue(newConfig.tls().keyStoreKeyPassword().isEmpty());
        assertTrue(newConfig.tls().trustStoreFile().isEmpty());
        assertTrue(newConfig.tls().trustStorePassword().isEmpty());
        assertTrue(newConfig.tls().trustStoreCertAlias().isEmpty());
        assertTrue(newConfig.tls().trustStoreFileType().isEmpty());
        assertTrue(newConfig.tls().trustStoreProvider().isEmpty());
    }

    @Test
    public void testCreateBuilderShortcuts() {
        OidcClientRegistrationConfig config = OidcClientRegistrationConfig
                .authServerUrl("auth-server-url")
                .metadata("Dynamic Client", "http://localhost:8081/protected/new-oidc-client-reg")
                .build();
        assertEquals("http://localhost:8081/protected/new-oidc-client-reg", config.metadata().redirectUri().orElse(null));
        assertEquals("Dynamic Client", config.metadata().clientName().orElse(null));

        config = OidcClientRegistrationConfig
                .registrationPath("registration-path")
                .metadata("redirect-uri")
                .build();
        assertEquals("registration-path", config.registrationPath().orElse(null));
        assertEquals("redirect-uri", config.metadata().redirectUri().orElse(null));
    }

    @Test
    public void testMetadataBuilderDefaults() {
        var metadata = new MetadataBuilder().build();
        assertTrue(metadata.clientName().isEmpty());
        assertTrue(metadata.postLogoutUri().isEmpty());
        assertTrue(metadata.redirectUri().isEmpty());
        assertTrue(metadata.extraProps().isEmpty());
    }
}
