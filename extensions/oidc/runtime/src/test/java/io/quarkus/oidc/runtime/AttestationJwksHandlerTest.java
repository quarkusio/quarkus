package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.vertx.http.runtime.security.ImmutablePathMatcher;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class AttestationJwksHandlerTest {

    @Test
    public void testJwksRouteOnlyForSelfAttestingTenants() throws Exception {
        AttestationKeyRegistry registry = new AttestationKeyRegistry();
        // Self-attesting tenants have an attestation key pair; remote-delegating tenants do not.
        registry.register("self", ecKeyPair(), ecKeyPair(), SignatureAlgorithm.ES256, 60);
        registry.register("remote", null, ecKeyPair(), SignatureAlgorithm.ES256, 60);

        OidcTenantConfig selfConfig = OidcTenantConfig.builder().tenantId("self").clientId("self-client").build();
        OidcTenantConfig remoteConfig = OidcTenantConfig.builder().tenantId("remote").clientId("remote-client").build();

        ImmutablePathMatcher<Handler<RoutingContext>> matcher = AttestationJwksHandler
                .buildPathMatcher(List.of(selfConfig, remoteConfig), registry, "/");

        assertNotNull(matcher);
        assertNotNull(matcher.match("/.well-known/attestation-jwks/self").getValue(),
                "Self-attesting tenant must expose its attestation JWKS");
        assertNull(matcher.match("/.well-known/attestation-jwks/remote").getValue(),
                "Remote-delegating tenant must not expose an attestation JWKS");
    }

    @Test
    public void testNoJwksRouteWhenAllTenantsDelegateToRemoteAttesters() throws Exception {
        AttestationKeyRegistry registry = new AttestationKeyRegistry();
        registry.register("remote", null, ecKeyPair(), SignatureAlgorithm.ES256, 60);

        OidcTenantConfig remoteConfig = OidcTenantConfig.builder().tenantId("remote").clientId("remote-client").build();

        ImmutablePathMatcher<Handler<RoutingContext>> matcher = AttestationJwksHandler
                .buildPathMatcher(List.of(remoteConfig), registry, "/");

        assertNull(matcher, "No attestation JWKS route must be registered when all tenants delegate to remote attesters");
    }

    private static KeyPair ecKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }
}
