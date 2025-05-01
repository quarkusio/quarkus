package io.quarkus.oidc.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.client.runtime.OidcClientConfig;
import io.quarkus.oidc.client.runtime.OidcClientConfig.Grant.Type;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig.Credentials.Jwt.Source;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig.Credentials.Secret.Method;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfigBuilder.CredentialsBuilder;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfigBuilder.JwtBuilder;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfigBuilder.SecretBuilder;

public class OidcClientConfigBuilderTest {

    @Test
    public void testDefaultValues() {
        var config = OidcClientConfig.builder().id("default-test").build();
        testDefaultValues(config);
        config = new OidcClientConfigBuilder().id("default-test").build();
        testDefaultValues(config);
    }

    private static void testDefaultValues(OidcClientConfig config) {
        // OidcClientConfig methods
        assertEquals("default-test", config.id().orElse(null));
        assertTrue(config.clientEnabled());
        assertTrue(config.scopes().isEmpty());
        assertTrue(config.refreshTokenTimeSkew().isEmpty());
        assertTrue(config.accessTokenExpiresIn().isEmpty());
        assertFalse(config.absoluteExpiresIn());
        assertTrue(config.grantOptions().isEmpty());
        assertTrue(config.earlyTokensAcquisition());
        assertTrue(config.headers().isEmpty());
        var grant = config.grant();
        assertNotNull(grant);
        assertEquals(Type.CLIENT, grant.type());
        assertEquals(OidcConstants.ACCESS_TOKEN_VALUE, grant.accessTokenProperty());
        assertEquals(OidcConstants.REFRESH_TOKEN_VALUE, grant.refreshTokenProperty());
        assertEquals(OidcConstants.EXPIRES_IN, grant.expiresInProperty());
        assertEquals(OidcConstants.REFRESH_EXPIRES_IN, grant.refreshExpiresInProperty());

        // OidcClientCommonConfig methods
        assertTrue(config.tokenPath().isEmpty());
        assertTrue(config.revokePath().isEmpty());
        assertTrue(config.clientId().isEmpty());
        assertTrue(config.clientName().isEmpty());
        var credentials = config.credentials();
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
        assertEquals(Source.CLIENT, jwt.source());
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
        var config = OidcClientConfig.builder()
                // OidcClientConfig methods
                .id("set-every-property-test")
                .clientEnabled(false)
                .scopes("one", "two")
                .scopes(List.of("three", "four"))
                .refreshTokenTimeSkew(Duration.ofSeconds(987))
                .accessTokenExpiresIn(Duration.ofSeconds(789))
                .absoluteExpiresIn(true)
                .grant()
                .type(Type.CODE)
                .accessTokenProperty("access_token_test")
                .refreshTokenProperty("refresh_token_test")
                .expiresInProperty("expires_in_test")
                .refreshExpiresInProperty("refresh_expires_in_test")
                .end()
                .grantOptions("one", "two", "three")
                .grantOptions("four", Map.of("five", "six"))
                .grantOptions(Map.of("seven", Map.of("eight", "nine")))
                .earlyTokensAcquisition(false)
                .headers("one", "two")
                .headers(Map.of("three", "four"))
                // OidcClientCommonConfig methods
                .tokenPath("token-path-yep")
                .revokePath("revoke-path-yep")
                .clientId("client-id-yep")
                .clientName("client-name-yep")
                .credentials()
                .secret("secret-yep")
                .clientSecret()
                .method(Method.QUERY)
                .value("value-yep")
                .provider("key-yep", "name-yep", "keyring-name-yep")
                .end()
                .jwt()
                .source(Source.BEARER)
                .tokenPath(Path.of("janitor"))
                .secretProvider()
                .keyringName("jwt-keyring-name-yep")
                .key("jwt-key-yep")
                .name("jwt-name-yep")
                .end()
                .secret("jwt-secret-yep")
                .key("jwt-key-yep")
                .keyFile("jwt-key-file-yep")
                .keyStoreFile("jwt-key-store-file-yep")
                .keyStorePassword("jwt-key-store-password-yep")
                .keyId("jwt-key-id-yep")
                .keyPassword("jwt-key-pwd-yep")
                .audience("jwt-audience-yep")
                .tokenKeyId("jwt-token-key-id-yep")
                .issuer("jwt-issuer")
                .subject("jwt-subject")
                .claim("claim-one-name", "claim-one-value")
                .claims(Map.of("claim-two-name", "claim-two-value"))
                .signatureAlgorithm("ES512")
                .lifespan(852)
                .assertion(true)
                .endCredentials()
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

        // OidcClientConfig methods
        assertEquals("set-every-property-test", config.id().orElse(null));
        assertFalse(config.clientEnabled());
        assertTrue(config.scopes().isPresent());
        List<String> scopes = config.scopes().get();
        assertEquals(4, scopes.size());
        assertTrue(scopes.contains("one"));
        assertTrue(scopes.contains("two"));
        assertTrue(scopes.contains("three"));
        assertTrue(scopes.contains("four"));
        assertEquals(987, config.refreshTokenTimeSkew().map(Duration::getSeconds).orElse(-1L));
        assertEquals(789, config.accessTokenExpiresIn().map(Duration::getSeconds).orElse(-1L));
        assertTrue(config.absoluteExpiresIn());
        var grant = config.grant();
        assertNotNull(grant);
        assertEquals(Type.CODE, grant.type());
        assertEquals("access_token_test", grant.accessTokenProperty());
        assertEquals("refresh_token_test", grant.refreshTokenProperty());
        assertEquals("expires_in_test", grant.expiresInProperty());
        assertEquals("refresh_expires_in_test", grant.refreshExpiresInProperty());
        var grantOptions = config.grantOptions();
        assertNotNull(grantOptions);
        assertEquals(3, grantOptions.size());
        assertTrue(grantOptions.containsKey("one"));
        assertEquals("three", grantOptions.get("one").get("two"));
        assertTrue(grantOptions.containsKey("four"));
        assertEquals("six", grantOptions.get("four").get("five"));
        assertTrue(grantOptions.containsKey("seven"));
        assertEquals("nine", grantOptions.get("seven").get("eight"));
        assertFalse(config.earlyTokensAcquisition());
        var headers = config.headers();
        assertNotNull(headers);
        assertEquals(2, headers.size());
        assertTrue(headers.containsKey("one"));
        assertEquals("two", headers.get("one"));
        assertTrue(headers.containsKey("three"));
        assertEquals("four", headers.get("three"));

        // OidcClientCommonConfig methods
        assertEquals("token-path-yep", config.tokenPath().orElse(null));
        assertEquals("revoke-path-yep", config.revokePath().orElse(null));
        assertEquals("client-id-yep", config.clientId().orElse(null));
        assertEquals("client-name-yep", config.clientName().orElse(null));
        var credentials = config.credentials();
        assertNotNull(credentials);
        assertEquals("secret-yep", credentials.secret().orElse(null));
        var clientSecret = credentials.clientSecret();
        assertNotNull(clientSecret);
        assertEquals(Method.QUERY, clientSecret.method().orElse(null));
        assertEquals("value-yep", clientSecret.value().orElse(null));
        var provider = clientSecret.provider();
        assertNotNull(provider);
        assertEquals("key-yep", provider.key().orElse(null));
        assertEquals("name-yep", provider.name().orElse(null));
        assertEquals("keyring-name-yep", provider.keyringName().orElse(null));
        var jwt = credentials.jwt();
        assertNotNull(jwt);
        assertEquals(Source.BEARER, jwt.source());
        assertEquals("jwt-secret-yep", jwt.secret().orElse(null));
        assertEquals("janitor", jwt.tokenPath().map(Path::toString).orElse(null));
        provider = jwt.secretProvider();
        assertNotNull(provider);
        assertEquals("jwt-keyring-name-yep", provider.keyringName().orElse(null));
        assertEquals("jwt-name-yep", provider.name().orElse(null));
        assertEquals("jwt-key-yep", provider.key().orElse(null));
        assertEquals("jwt-key-yep", jwt.key().orElse(null));
        assertEquals("jwt-key-file-yep", jwt.keyFile().orElse(null));
        assertEquals("jwt-key-store-file-yep", jwt.keyStoreFile().orElse(null));
        assertEquals("jwt-key-store-password-yep", jwt.keyStorePassword().orElse(null));
        assertEquals("jwt-key-id-yep", jwt.keyId().orElse(null));
        assertEquals("jwt-key-pwd-yep", jwt.keyPassword().orElse(null));
        assertEquals("jwt-audience-yep", jwt.audience().orElse(null));
        assertEquals("jwt-token-key-id-yep", jwt.tokenKeyId().orElse(null));
        assertEquals("jwt-issuer", jwt.issuer().orElse(null));
        assertEquals("jwt-subject", jwt.subject().orElse(null));
        var claims = jwt.claims();
        assertNotNull(claims);
        assertEquals(2, claims.size());
        assertTrue(claims.containsKey("claim-one-name"));
        assertEquals("claim-one-value", claims.get("claim-one-name"));
        assertTrue(claims.containsKey("claim-two-name"));
        assertEquals("claim-two-value", claims.get("claim-two-name"));
        assertEquals("ES512", jwt.signatureAlgorithm().orElse(null));
        assertEquals(852, jwt.lifespan());
        assertTrue(jwt.assertion());

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
        var previousConfig = OidcClientConfig.builder()
                .id("copy-proxy-properties-test")
                .proxy("need", 55, "no", "education")
                .build();
        var newConfig = OidcClientConfig.builder(previousConfig)
                .proxy("fast-car", 22)
                .build();

        assertNotNull(previousConfig.proxy());
        assertEquals("copy-proxy-properties-test", newConfig.id().orElse(null));
        assertEquals("fast-car", newConfig.proxy().host().orElse(null));
        assertEquals(22, newConfig.proxy().port());
        assertEquals("no", newConfig.proxy().username().orElse(null));
        assertEquals("education", newConfig.proxy().password().orElse(null));
    }

    @Test
    public void testCopyOidcClientConfigProperties() {
        var existingConfig = OidcClientConfig.builder()
                // OidcClientConfig methods
                .id("test-copy-client-props")
                .clientEnabled(false)
                .scopes("one", "two")
                .scopes(List.of("three", "four"))
                .refreshTokenTimeSkew(Duration.ofSeconds(987))
                .accessTokenExpiresIn(Duration.ofSeconds(789))
                .absoluteExpiresIn(true)
                .grant()
                .type(Type.CODE)
                .accessTokenProperty("access_token_test")
                .refreshTokenProperty("refresh_token_test")
                .expiresInProperty("expires_in_test")
                .refreshExpiresInProperty("refresh_expires_in_test")
                .end()
                .grantOptions("one", "two", "three")
                .grantOptions("four", Map.of("five", "six"))
                .grantOptions(Map.of("seven", Map.of("eight", "nine")))
                .earlyTokensAcquisition(false)
                .headers("one", "two")
                .headers(Map.of("three", "four"))
                .build();

        // OidcClientConfig methods
        assertEquals("test-copy-client-props", existingConfig.id().orElse(null));
        assertFalse(existingConfig.clientEnabled());
        assertTrue(existingConfig.scopes().isPresent());
        List<String> scopes = existingConfig.scopes().get();
        assertEquals(4, scopes.size());
        assertTrue(scopes.contains("one"));
        assertTrue(scopes.contains("two"));
        assertTrue(scopes.contains("three"));
        assertTrue(scopes.contains("four"));
        assertEquals(987, existingConfig.refreshTokenTimeSkew().map(Duration::getSeconds).orElse(-1L));
        assertEquals(789, existingConfig.accessTokenExpiresIn().map(Duration::getSeconds).orElse(-1L));
        assertTrue(existingConfig.absoluteExpiresIn());
        var grant = existingConfig.grant();
        assertNotNull(grant);
        assertEquals(Type.CODE, grant.type());
        assertEquals("access_token_test", grant.accessTokenProperty());
        assertEquals("refresh_token_test", grant.refreshTokenProperty());
        assertEquals("expires_in_test", grant.expiresInProperty());
        assertEquals("refresh_expires_in_test", grant.refreshExpiresInProperty());
        var grantOptions = existingConfig.grantOptions();
        assertNotNull(grantOptions);
        assertEquals(3, grantOptions.size());
        assertTrue(grantOptions.containsKey("one"));
        assertEquals("three", grantOptions.get("one").get("two"));
        assertTrue(grantOptions.containsKey("four"));
        assertEquals("six", grantOptions.get("four").get("five"));
        assertTrue(grantOptions.containsKey("seven"));
        assertEquals("nine", grantOptions.get("seven").get("eight"));
        assertFalse(existingConfig.earlyTokensAcquisition());
        var headers = existingConfig.headers();
        assertNotNull(headers);
        assertEquals(2, headers.size());
        assertTrue(headers.containsKey("one"));
        assertEquals("two", headers.get("one"));
        assertTrue(headers.containsKey("three"));
        assertEquals("four", headers.get("three"));

        var newConfig = OidcClientConfig.builder(existingConfig)
                // OidcClientConfig methods
                .clientEnabled(true)
                .scopes("five", "six")
                .accessTokenExpiresIn(Duration.ofSeconds(444))
                .grant()
                .accessTokenProperty("access_token_test-CHANGED")
                .expiresInProperty("expires_in_test-CHANGED")
                .end()
                .earlyTokensAcquisition(true)
                .build();

        // OidcClientConfig methods
        assertEquals("test-copy-client-props", newConfig.id().orElse(null));
        assertTrue(newConfig.clientEnabled());
        assertTrue(newConfig.scopes().isPresent());
        scopes = newConfig.scopes().get();
        assertEquals(6, scopes.size());
        assertTrue(scopes.contains("one"));
        assertTrue(scopes.contains("two"));
        assertTrue(scopes.contains("three"));
        assertTrue(scopes.contains("four"));
        assertTrue(scopes.contains("five"));
        assertTrue(scopes.contains("six"));
        assertEquals(987, newConfig.refreshTokenTimeSkew().map(Duration::getSeconds).orElse(-1L));
        assertEquals(444, newConfig.accessTokenExpiresIn().map(Duration::getSeconds).orElse(-1L));
        assertTrue(newConfig.absoluteExpiresIn());
        grant = newConfig.grant();
        assertNotNull(grant);
        assertEquals(Type.CODE, grant.type());
        assertEquals("access_token_test-CHANGED", grant.accessTokenProperty());
        assertEquals("refresh_token_test", grant.refreshTokenProperty());
        assertEquals("expires_in_test-CHANGED", grant.expiresInProperty());
        assertEquals("refresh_expires_in_test", grant.refreshExpiresInProperty());
        grantOptions = newConfig.grantOptions();
        assertNotNull(grantOptions);
        assertEquals(3, grantOptions.size());
        assertTrue(grantOptions.containsKey("one"));
        assertEquals("three", grantOptions.get("one").get("two"));
        assertTrue(grantOptions.containsKey("four"));
        assertEquals("six", grantOptions.get("four").get("five"));
        assertTrue(grantOptions.containsKey("seven"));
        assertEquals("nine", grantOptions.get("seven").get("eight"));
        assertTrue(newConfig.earlyTokensAcquisition());
        headers = newConfig.headers();
        assertNotNull(headers);
        assertEquals(2, headers.size());
        assertTrue(headers.containsKey("one"));
        assertEquals("two", headers.get("one"));
        assertTrue(headers.containsKey("three"));
        assertEquals("four", headers.get("three"));
    }

    @Test
    public void testCopyOidcClientCommonConfigProperties() {
        var existingConfig = OidcClientConfig.builder()
                // OidcClientConfig methods
                .id("copy-oidc-client-common-props")
                // OidcClientCommonConfig methods
                .tokenPath("token-path-yep")
                .revokePath("revoke-path-yep")
                .clientId("client-id-yep")
                .clientName("client-name-yep")
                .credentials()
                .secret("secret-yep")
                .clientSecret()
                .method(Method.QUERY)
                .value("value-yep")
                .provider("key-yep", "name-yep", "keyring-name-yep")
                .end()
                .jwt()
                .source(Source.BEARER)
                .tokenPath(Path.of("robot"))
                .secretProvider()
                .keyringName("jwt-keyring-name-yep")
                .key("jwt-key-yep")
                .name("jwt-name-yep")
                .end()
                .secret("jwt-secret-yep")
                .key("jwt-key-yep")
                .keyFile("jwt-key-file-yep")
                .keyStoreFile("jwt-key-store-file-yep")
                .keyStorePassword("jwt-key-store-password-yep")
                .keyId("jwt-key-id-yep")
                .keyPassword("jwt-key-pwd-yep")
                .audience("jwt-audience-yep")
                .tokenKeyId("jwt-token-key-id-yep")
                .issuer("jwt-issuer")
                .subject("jwt-subject")
                .claim("claim-one-name", "claim-one-value")
                .claims(Map.of("claim-two-name", "claim-two-value"))
                .signatureAlgorithm("ES512")
                .lifespan(852)
                .assertion(true)
                .endCredentials()
                .build();

        assertEquals("copy-oidc-client-common-props", existingConfig.id().orElse(null));

        // OidcClientCommonConfig methods
        assertEquals("token-path-yep", existingConfig.tokenPath().orElse(null));
        assertEquals("revoke-path-yep", existingConfig.revokePath().orElse(null));
        assertEquals("client-id-yep", existingConfig.clientId().orElse(null));
        assertEquals("client-name-yep", existingConfig.clientName().orElse(null));
        var credentials = existingConfig.credentials();
        assertNotNull(credentials);
        assertEquals("secret-yep", credentials.secret().orElse(null));
        var clientSecret = credentials.clientSecret();
        assertNotNull(clientSecret);
        assertEquals(Method.QUERY, clientSecret.method().orElse(null));
        assertEquals("value-yep", clientSecret.value().orElse(null));
        var provider = clientSecret.provider();
        assertNotNull(provider);
        assertEquals("key-yep", provider.key().orElse(null));
        assertEquals("name-yep", provider.name().orElse(null));
        assertEquals("keyring-name-yep", provider.keyringName().orElse(null));
        var jwt = credentials.jwt();
        assertNotNull(jwt);
        assertEquals(Source.BEARER, jwt.source());
        assertEquals("jwt-secret-yep", jwt.secret().orElse(null));
        assertEquals("robot", jwt.tokenPath().map(Path::toString).orElse(null));
        provider = jwt.secretProvider();
        assertNotNull(provider);
        assertEquals("jwt-keyring-name-yep", provider.keyringName().orElse(null));
        assertEquals("jwt-name-yep", provider.name().orElse(null));
        assertEquals("jwt-key-yep", provider.key().orElse(null));
        assertEquals("jwt-key-yep", jwt.key().orElse(null));
        assertEquals("jwt-key-file-yep", jwt.keyFile().orElse(null));
        assertEquals("jwt-key-store-file-yep", jwt.keyStoreFile().orElse(null));
        assertEquals("jwt-key-store-password-yep", jwt.keyStorePassword().orElse(null));
        assertEquals("jwt-key-id-yep", jwt.keyId().orElse(null));
        assertEquals("jwt-key-pwd-yep", jwt.keyPassword().orElse(null));
        assertEquals("jwt-audience-yep", jwt.audience().orElse(null));
        assertEquals("jwt-token-key-id-yep", jwt.tokenKeyId().orElse(null));
        assertEquals("jwt-issuer", jwt.issuer().orElse(null));
        assertEquals("jwt-subject", jwt.subject().orElse(null));
        var claims = jwt.claims();
        assertNotNull(claims);
        assertEquals(2, claims.size());
        assertTrue(claims.containsKey("claim-one-name"));
        assertEquals("claim-one-value", claims.get("claim-one-name"));
        assertTrue(claims.containsKey("claim-two-name"));
        assertEquals("claim-two-value", claims.get("claim-two-name"));
        assertEquals("ES512", jwt.signatureAlgorithm().orElse(null));
        assertEquals(852, jwt.lifespan());
        assertTrue(jwt.assertion());

        var newConfig = OidcClientConfig.builder(existingConfig)
                // OidcClientCommonConfig methods
                .tokenPath("token-path-yep-CHANGED")
                .clientId("client-id-yep-CHANGED")
                .credentials()
                .secret("secret-yep-CHANGED")
                .clientSecret("val-1", Method.POST_JWT)
                .jwt()
                .secret("different-secret")
                .secretProvider()
                .key("jwt-key-yep-CHANGED")
                .end()
                .key("jwt-key-yep-CHANGED-2")
                .keyStoreFile("jwt-key-store-file-yep-CHANGED")
                .keyPassword("jwt-key-pwd-yep-CHANGED")
                .issuer("jwt-issuer-CHANGED")
                .claim("aaa", "bbb")
                .lifespan(333)
                .end()
                .clientSecret("val-1", Method.POST_JWT)
                .end()
                .build();

        assertEquals("copy-oidc-client-common-props", newConfig.id().orElse(null));

        // OidcClientCommonConfig methods
        assertEquals("token-path-yep-CHANGED", newConfig.tokenPath().orElse(null));
        assertEquals("revoke-path-yep", newConfig.revokePath().orElse(null));
        assertEquals("client-id-yep-CHANGED", newConfig.clientId().orElse(null));
        assertEquals("client-name-yep", newConfig.clientName().orElse(null));
        credentials = newConfig.credentials();
        assertNotNull(credentials);
        assertEquals("secret-yep-CHANGED", credentials.secret().orElse(null));
        clientSecret = credentials.clientSecret();
        assertNotNull(clientSecret);
        assertEquals(Method.POST_JWT, clientSecret.method().orElse(null));
        assertEquals("val-1", clientSecret.value().orElse(null));
        provider = clientSecret.provider();
        assertNotNull(provider);
        assertEquals("key-yep", provider.key().orElse(null));
        assertEquals("name-yep", provider.name().orElse(null));
        assertEquals("keyring-name-yep", provider.keyringName().orElse(null));
        jwt = credentials.jwt();
        assertNotNull(jwt);
        assertEquals(Source.BEARER, jwt.source());
        assertEquals("different-secret", jwt.secret().orElse(null));
        provider = jwt.secretProvider();
        assertNotNull(provider);
        assertEquals("jwt-keyring-name-yep", provider.keyringName().orElse(null));
        assertEquals("jwt-name-yep", provider.name().orElse(null));
        assertEquals("jwt-key-yep-CHANGED", provider.key().orElse(null));
        assertEquals("jwt-key-yep-CHANGED-2", jwt.key().orElse(null));
        assertEquals("jwt-key-file-yep", jwt.keyFile().orElse(null));
        assertEquals("jwt-key-store-file-yep-CHANGED", jwt.keyStoreFile().orElse(null));
        assertEquals("jwt-key-store-password-yep", jwt.keyStorePassword().orElse(null));
        assertEquals("jwt-key-id-yep", jwt.keyId().orElse(null));
        assertEquals("jwt-key-pwd-yep-CHANGED", jwt.keyPassword().orElse(null));
        assertEquals("jwt-audience-yep", jwt.audience().orElse(null));
        assertEquals("jwt-token-key-id-yep", jwt.tokenKeyId().orElse(null));
        assertEquals("jwt-issuer-CHANGED", jwt.issuer().orElse(null));
        assertEquals("jwt-subject", jwt.subject().orElse(null));
        claims = jwt.claims();
        assertNotNull(claims);
        assertEquals(3, claims.size());
        assertTrue(claims.containsKey("claim-one-name"));
        assertEquals("claim-one-value", claims.get("claim-one-name"));
        assertTrue(claims.containsKey("claim-two-name"));
        assertEquals("claim-two-value", claims.get("claim-two-name"));
        assertTrue(claims.containsKey("aaa"));
        assertEquals("bbb", claims.get("aaa"));
        assertEquals("ES512", jwt.signatureAlgorithm().orElse(null));
        assertEquals(333, jwt.lifespan());
        assertTrue(jwt.assertion());
    }

    @Test
    public void testCopyOidcCommonConfigProperties() {
        var previousConfig = OidcClientConfig.builder()
                .id("common-props-test")
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
        var newConfig = OidcClientConfig.builder(previousConfig)
                .discoveryEnabled(true)
                .connectionDelay(Duration.ofSeconds(753))
                .connectionTimeout(Duration.ofSeconds(357))
                .maxPoolSize(1988)
                .proxy("cross", 44, "the", "boarder")
                .build();

        assertEquals("common-props-test", newConfig.id().orElse(null));
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
        OidcClientConfig config = OidcClientConfig.authServerUrl("auth-server-url").id("shortcuts-1").build();
        assertEquals("auth-server-url", config.authServerUrl().orElse(null));
        assertEquals("shortcuts-1", config.id().orElse(null));

        config = OidcClientConfig.registrationPath("registration-path").id("shortcuts-2").build();
        assertEquals("registration-path", config.registrationPath().orElse(null));
        assertEquals("shortcuts-2", config.id().orElse(null));

        config = OidcClientConfig.tokenPath("token-path").id("shortcuts-3").build();
        assertEquals("token-path", config.tokenPath().orElse(null));
        assertEquals("shortcuts-3", config.id().orElse(null));
    }

    @Test
    public void testCredentialsBuilder() {
        var jwt = new JwtBuilder<>()
                .secret("hush-hush")
                .build();
        var clientSecret = new SecretBuilder<>()
                .value("harry")
                .build();
        var credentials = new CredentialsBuilder<>()
                .secret("1234")
                .jwt(jwt)
                .clientSecret(clientSecret)
                .build();
        var config = OidcClientConfig.builder().id("1").credentials(credentials).build();
        var buildCredentials = config.credentials();
        assertEquals("1", config.id().orElse(null));
        assertNotNull(buildCredentials);
        assertEquals("1234", buildCredentials.secret().orElse(null));
        assertEquals("hush-hush", buildCredentials.jwt().secret().orElse(null));
        assertEquals("harry", buildCredentials.clientSecret().value().orElse(null));
    }

    @Test
    public void testGrantBuilder() {
        var grant = new OidcClientConfigBuilder.GrantBuilder().build();
        // tests defaults
        assertEquals(Type.CLIENT, grant.type());
        assertEquals(OidcConstants.ACCESS_TOKEN_VALUE, grant.accessTokenProperty());
        assertEquals(OidcConstants.REFRESH_EXPIRES_IN, grant.refreshExpiresInProperty());
        assertEquals(OidcConstants.REFRESH_TOKEN_VALUE, grant.refreshTokenProperty());
        assertEquals(OidcConstants.EXPIRES_IN, grant.expiresInProperty());

        grant = new OidcClientConfigBuilder.GrantBuilder()
                .type(Type.CIBA)
                .expiresInProperty("exp1")
                .accessTokenProperty("acc1")
                .refreshExpiresInProperty("exp2")
                .refreshTokenProperty("ref1")
                .build();
        var config = OidcClientConfig.builder().id("2").grant(grant).build();
        var buildGrant = config.grant();
        assertEquals("2", config.id().orElse(null));
        assertNotNull(buildGrant);
        assertEquals(Type.CIBA, buildGrant.type());
        assertEquals("exp1", buildGrant.expiresInProperty());
        assertEquals("acc1", buildGrant.accessTokenProperty());
        assertEquals("exp2", buildGrant.refreshExpiresInProperty());
        assertEquals("ref1", buildGrant.refreshTokenProperty());
    }
}
