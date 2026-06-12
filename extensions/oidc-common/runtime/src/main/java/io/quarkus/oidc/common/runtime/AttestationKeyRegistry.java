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

import io.quarkus.oidc.common.ClientAttester.KeyOwnershipProof;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@Singleton
public class AttestationKeyRegistry {

    private final Map<String, AttestationJwtContext> contexts = new ConcurrentHashMap<>();

    public AttestationJwtContext register(String clientId, KeyPair keyPair,
            SignatureAlgorithm signatureAlgorithm, int lifespan) {
        AttestationJwtContext akp = new AttestationJwtContext(keyPair, signatureAlgorithm, lifespan);
        contexts.put(clientId, akp);
        return akp;
    }

    public AttestationJwtContext getAttestationJwtContext(String clientId) {
        return contexts.get(clientId);
    }

    public void remove(String clientId) {
        contexts.remove(clientId);
    }

    public String getJwkSet(String clientId) {
        JsonArray keys = new JsonArray();
        AttestationJwtContext akp = contexts.get(clientId);
        if (akp != null) {
            JsonObject jwk = new JsonObject(convertPublicKeyToJwk(akp.keyPair().getPublic()));
            jwk.put("kid", akp.kid());
            keys.add(jwk);
        }
        return new JsonObject().put("keys", keys).toString();
    }

    public KeyOwnershipProof getKeyOwnershipProof(String clientId) {
        AttestationJwtContext ctx = contexts.get(clientId);
        if (ctx == null) {
            return null;
        }
        return new KeyOwnershipProof(
                new JsonObject(convertPublicKeyToJwk(ctx.keyPair().getPublic())),
                ctx.keyPair().getPrivate(),
                ctx.signatureAlgorithm());
    }

    public static Map<String, Object> convertPublicKeyToJwk(PublicKey key) {
        try {
            Map<String, Object> params = new java.util.HashMap<>(
                    PublicJsonWebKey.Factory.newPublicJwk(key).toParams(OutputControlLevel.PUBLIC_ONLY));
            params.put("alg", SignatureAlgorithm.ES256.getAlgorithm());
            return params;
        } catch (JoseException ex) {
            throw new RuntimeException("Failed to convert public key to JWK", ex);
        }
    }

    public record AttestationJwtContext(String kid, KeyPair keyPair,
            SignatureAlgorithm signatureAlgorithm, int lifespan) {
        AttestationJwtContext(KeyPair keyPair, SignatureAlgorithm signatureAlgorithm, int lifespan) {
            this(UUID.randomUUID().toString(), keyPair, signatureAlgorithm, lifespan);
        }
    }
}
