package io.quarkus.oidc.runtime;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Singleton;

import org.jose4j.jwk.JsonWebKey.OutputControlLevel;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.lang.JoseException;

import io.quarkus.oidc.common.ClientAttester.AttestationSigningKey;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Holds OIDC tenant specific attestation contexts, including key pairs, signature algorithms,
 * and key identifiers used for building attestation and proof-of-possession JWTs.
 * <p>
 * Self-attesting {@link io.quarkus.oidc.common.ClientAttester}s receive the attestation signing key in the
 * {@link io.quarkus.oidc.common.ClientAttester.ClientAttestationContext} and must not depend on this class.
 */
@Singleton
public class AttestationKeyRegistry {

    private final Map<String, AttestationJwtContext> contexts = new ConcurrentHashMap<>();

    AttestationJwtContext register(String tenantId, KeyPair attestationKeyPair, KeyPair instanceKeyPair,
            SignatureAlgorithm signatureAlgorithm, int lifespan) {
        AttestationJwtContext akp = new AttestationJwtContext(attestationKeyPair, instanceKeyPair,
                signatureAlgorithm, lifespan);
        contexts.put(tenantId, akp);
        return akp;
    }

    /**
     * Returns the attestation signing key a self-attesting {@link io.quarkus.oidc.common.ClientAttester}
     * signs the Client Attestation JWT with.
     *
     * @return the attestation signing key, or {@code null} if the context has no attestation key pair
     *         because the attester delegates to a remote attestation service
     */
    static AttestationSigningKey getAttestationSigningKey(AttestationJwtContext ctx) {
        if (ctx.attestationKeyPair() == null) {
            return null;
        }
        return new AttestationSigningKey(ctx.attestationKeyPair().getPrivate(), ctx.kid(), ctx.signatureAlgorithm(),
                ctx.lifespan());
    }

    /**
     * Checks if the tenant self-attests the client and therefore has an attestation public key to publish.
     */
    boolean hasAttestationKey(String tenantId) {
        AttestationJwtContext ctx = contexts.get(tenantId);
        // The attestation key pair is only generated for self-attesting tenants.
        return ctx != null && ctx.attestationKeyPair() != null;
    }

    /**
     * Removes the attestation context of the tenant, but only if it is the context which is currently registered.
     * A dynamic tenant can be replaced while its previous provider client is still serving in-flight requests,
     * in which case the previous client closing must not evict the context registered by its replacement.
     */
    void remove(String tenantId, AttestationJwtContext ctx) {
        contexts.remove(tenantId, ctx);
    }

    String getJwkSet(String tenantId) {
        JsonArray keys = new JsonArray();
        AttestationJwtContext akp = contexts.get(tenantId);
        // Only self-attesting tenants have an attestation key pair to publish.
        if (akp != null && akp.attestationKeyPair() != null) {
            JsonObject jwk = new JsonObject(
                    convertPublicKeyToJwk(akp.attestationKeyPair().getPublic(), akp.signatureAlgorithm()));
            jwk.put("kid", akp.kid());
            keys.add(jwk);
        }
        return new JsonObject().put("keys", keys).toString();
    }

    static JsonObject getClientPublicKeyJwk(AttestationJwtContext ctx) {
        return new JsonObject(convertPublicKeyToJwk(ctx.instanceKeyPair().getPublic(), ctx.signatureAlgorithm()));
    }

    static Map<String, Object> convertPublicKeyToJwk(PublicKey key, SignatureAlgorithm algorithm) {
        try {
            Map<String, Object> params = new java.util.HashMap<>(
                    PublicJsonWebKey.Factory.newPublicJwk(key).toParams(OutputControlLevel.PUBLIC_ONLY));
            params.put("alg", algorithm.getAlgorithm());
            return params;
        } catch (JoseException ex) {
            throw new RuntimeException("Failed to convert public key to JWK", ex);
        }
    }

    record AttestationJwtContext(String kid, KeyPair attestationKeyPair, KeyPair instanceKeyPair,
            SignatureAlgorithm signatureAlgorithm, int lifespan) {
        AttestationJwtContext(KeyPair attestationKeyPair, KeyPair instanceKeyPair,
                SignatureAlgorithm signatureAlgorithm, int lifespan) {
            this(UUID.randomUUID().toString(), attestationKeyPair, instanceKeyPair, signatureAlgorithm, lifespan);
        }
    }
}
