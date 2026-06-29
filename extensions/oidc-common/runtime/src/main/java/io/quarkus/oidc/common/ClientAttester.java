package io.quarkus.oidc.common;

import java.security.PrivateKey;

import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;

/**
 * Creates a Client Attestation JWT for OAuth 2.0 Attestation-Based Client Authentication.
 * <p>
 * This is an experimental API that may change in future releases.
 */
public interface ClientAttester {

    /**
     * @param publicKeyJwk the client's public key in JWK format
     * @param proofSigningKey the private key for signing a key ownership proof
     * @param signatureAlgorithm the signature algorithm to use
     */
    record KeyOwnershipProof(JsonObject publicKeyJwk, PrivateKey proofSigningKey,
            SignatureAlgorithm signatureAlgorithm) {
    }

    /**
     * Context provided to {@link ClientAttester#attest(ClientAttestationContext)}.
     *
     * @param clientId the OAuth 2.0 client identifier
     * @param keyOwnershipProof the cryptographic material for proving key ownership
     */
    record ClientAttestationContext(String clientId, KeyOwnershipProof keyOwnershipProof) {
    }

    Uni<String> attest(ClientAttestationContext context);
}
