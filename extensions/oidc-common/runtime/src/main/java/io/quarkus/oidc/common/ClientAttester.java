package io.quarkus.oidc.common;

import io.smallrye.common.annotation.Experimental;
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
     */
    record ClientAttestationContext(String clientId, JsonObject clientPublicKeyJwk) {
    }

    Uni<String> attest(ClientAttestationContext context);
}
