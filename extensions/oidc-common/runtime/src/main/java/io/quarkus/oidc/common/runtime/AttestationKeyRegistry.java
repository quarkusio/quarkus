package io.quarkus.oidc.common.runtime;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Singleton;

import org.jose4j.jwk.JsonWebKey.OutputControlLevel;
import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.lang.JoseException;

import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Holds OIDC tenant specific attestation contexts, including key pairs, signature algorithms,
 * and key identifiers used for building attestation and proof-of-possession JWTs.
 */
@Singleton
public class AttestationKeyRegistry {

    private final Map<String, AttestationJwtContext> contexts = new ConcurrentHashMap<>();

    public AttestationJwtContext register(String tenantId, KeyPair attestationKeyPair, KeyPair instanceKeyPair,
            SignatureAlgorithm signatureAlgorithm, int lifespan) {
        AttestationJwtContext akp = new AttestationJwtContext(attestationKeyPair, instanceKeyPair,
                signatureAlgorithm, lifespan);
        contexts.put(tenantId, akp);
        return akp;
    }

    public AttestationJwtContext getAttestationJwtContext(String tenantId) {
        return contexts.get(tenantId);
    }

    public void remove(String tenantId) {
        contexts.remove(tenantId);
    }

    public String getJwkSet(String tenantId) {
        JsonArray keys = new JsonArray();
        AttestationJwtContext akp = contexts.get(tenantId);
        if (akp != null) {
            JsonObject jwk = new JsonObject(
                    convertPublicKeyToJwk(akp.attestationKeyPair().getPublic(), akp.signatureAlgorithm()));
            jwk.put("kid", akp.kid());
            keys.add(jwk);
        }
        return new JsonObject().put("keys", keys).toString();
    }

    public JsonObject getClientPublicKeyJwk(String tenantId) {
        AttestationJwtContext ctx = contexts.get(tenantId);
        if (ctx == null) {
            return null;
        }
        return new JsonObject(convertPublicKeyToJwk(ctx.instanceKeyPair().getPublic(), ctx.signatureAlgorithm()));
    }

    public static Map<String, Object> convertPublicKeyToJwk(PublicKey key, SignatureAlgorithm algorithm) {
        try {
            Map<String, Object> params = new java.util.HashMap<>(
                    PublicJsonWebKey.Factory.newPublicJwk(key).toParams(OutputControlLevel.PUBLIC_ONLY));
            params.put("alg", algorithm.getAlgorithm());
            return params;
        } catch (JoseException ex) {
            throw new RuntimeException("Failed to convert public key to JWK", ex);
        }
    }

    public record AttestationJwtContext(String kid, KeyPair attestationKeyPair, KeyPair instanceKeyPair,
            SignatureAlgorithm signatureAlgorithm, int lifespan) {
        AttestationJwtContext(KeyPair attestationKeyPair, KeyPair instanceKeyPair,
                SignatureAlgorithm signatureAlgorithm, int lifespan) {
            this(UUID.randomUUID().toString(), attestationKeyPair, instanceKeyPair, signatureAlgorithm, lifespan);
        }
    }
}
