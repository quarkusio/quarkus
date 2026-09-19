package io.quarkus.oidc.runtime;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Singleton;

import org.jose4j.jwk.JsonWebKey.OutputControlLevel;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.lang.JoseException;

import io.smallrye.common.annotation.Experimental;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Holds OIDC tenant specific attestation contexts, including key pairs, signature algorithms,
 * and key identifiers used for building attestation and proof-of-possession JWTs.
 */
@Experimental("This API is currently experimental and might get changed")
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
     * Returns the attestation signing key for a self-attesting custom {@link io.quarkus.oidc.common.ClientAttester}
     * to sign the Client Attestation JWT with.
     *
     * @param tenantId the OIDC tenant id, passed to the attester under the {@link OidcUtils#TENANT_ID_ATTRIBUTE} key
     * @return the attestation signing key, or {@code null} if the tenant does not self-attest the client
     */
    public AttestationSigningKey getAttestationSigningKey(String tenantId) {
        AttestationJwtContext ctx = contexts.get(tenantId);
        // The attestation key pair is only generated for self-attesting tenants.
        if (ctx == null || ctx.attestationKeyPair() == null) {
            return null;
        }
        return new AttestationSigningKey(ctx.attestationKeyPair().getPrivate(), ctx.kid(), ctx.signatureAlgorithm(),
                ctx.lifespan());
    }

    AttestationJwtContext getAttestationJwtContext(String tenantId) {
        return contexts.get(tenantId);
    }

    void remove(String tenantId) {
        contexts.remove(tenantId);
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

    JsonObject getClientPublicKeyJwk(String tenantId) {
        AttestationJwtContext ctx = contexts.get(tenantId);
        if (ctx == null) {
            return null;
        }
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

    /**
     * The attestation signing key a self-attesting custom {@link io.quarkus.oidc.common.ClientAttester} uses to sign
     * the Client Attestation JWT.
     *
     * @param signingKey the attestation private key
     * @param kid the key identifier which must be set as the {@code kid} JWT header
     * @param signatureAlgorithm the signature algorithm which must be used to sign the JWT
     * @param lifespan the recommended attestation JWT lifespan in seconds
     */
    public record AttestationSigningKey(PrivateKey signingKey, String kid, SignatureAlgorithm signatureAlgorithm,
            int lifespan) {
    }

    record AttestationJwtContext(String kid, KeyPair attestationKeyPair, KeyPair instanceKeyPair,
            SignatureAlgorithm signatureAlgorithm, int lifespan) {
        AttestationJwtContext(KeyPair attestationKeyPair, KeyPair instanceKeyPair,
                SignatureAlgorithm signatureAlgorithm, int lifespan) {
            this(UUID.randomUUID().toString(), attestationKeyPair, instanceKeyPair, signatureAlgorithm, lifespan);
        }
    }
}
