package io.quarkus.oidc.common;

import java.security.PrivateKey;
import java.util.Map;

import io.smallrye.common.annotation.Experimental;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;

/**
 * Creates a Client Attestation JWT for OAuth2 Attestation-Based Client Authentication.
 */
@Experimental("This API is currently experimental and might get changed")
public interface ClientAttester {

    /**
     * Context provided to {@link ClientAttester#attest(ClientAttestationContext)}.
     *
     * @param clientId the OAuth2 client identifier
     * @param clientPublicKeyJwk the client's public key in JWK format.
     *        Key ownership is confirmed by the attestation proof-of-possession (PoP) JWT.
     * @param attestationSigningKey the attestation signing key a self-attesting attester must sign the
     *        Client Attestation JWT with, or {@code null} if this attester does not self-attest the client
     * @param extraParams additional implementation specific parameters
     */
    record ClientAttestationContext(String clientId, JsonObject clientPublicKeyJwk,
            AttestationSigningKey attestationSigningKey, Map<String, Object> extraParams) {
    }

    /**
     * The attestation signing key a self-attesting attester uses to sign the Client Attestation JWT.
     * Its public key is published by Quarkus at the attestation JWKS endpoint.
     *
     * @param signingKey the attestation private key
     * @param kid the key identifier which must be set as the {@code kid} JWT header
     * @param signatureAlgorithm the signature algorithm which must be used to sign the JWT
     * @param lifespan the recommended attestation JWT lifespan in seconds
     */
    record AttestationSigningKey(PrivateKey signingKey, String kid, SignatureAlgorithm signatureAlgorithm,
            int lifespan) {
    }

    Uni<String> attest(ClientAttestationContext context);

    /**
     * Indicates whether this attester self-attests the client by signing the Client Attestation JWT with the
     * attestation key pair managed by Quarkus, as opposed to delegating to a remote attestation service.
     * <p>
     * Self-attesting attesters must return {@code true} so that Quarkus publishes the attestation public key at the
     * attestation JWKS endpoint, allowing the OAuth2 authorization server to verify the Client Attestation JWT.
     * Attesters that delegate to a remote attestation service must return {@code false} (the default), in which case
     * no attestation public key is published for the tenant.
     *
     * @return {@code true} if this attester self-attests the client, {@code false} otherwise
     */
    default boolean selfAttesting() {
        return false;
    }
}
