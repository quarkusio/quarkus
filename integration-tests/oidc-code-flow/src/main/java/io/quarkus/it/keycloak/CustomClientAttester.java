package io.quarkus.it.keycloak;

import jakarta.inject.Singleton;

import io.quarkus.oidc.TenantFeature;
import io.quarkus.oidc.common.ClientAttester;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;

@Singleton
@TenantFeature("tenant-attestation-custom")
public class CustomClientAttester implements ClientAttester {

    @Override
    public Uni<String> attest(ClientAttestationContext context) {
        KeyOwnershipProof proof = context.keyOwnershipProof();
        if (proof == null) {
            return Uni.createFrom().failure(
                    new RuntimeException(
                            "No key ownership proof is available for client: " + context.clientId()));
        }
        JsonObject cnf = new JsonObject().put("jwk", proof.publicKeyJwk().getMap());
        String jwt = Jwt.issuer(context.clientId())
                .subject(context.clientId())
                .claim("cnf", cnf.getMap())
                .expiresIn(10)
                .jws()
                .type(OidcConstants.CLIENT_ATTESTATION_JWT_TYPE)
                .algorithm(proof.signatureAlgorithm())
                .sign(proof.proofSigningKey());
        return Uni.createFrom().item(jwt);
    }
}
