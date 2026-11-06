package io.quarkus.it.keycloak;

import jakarta.inject.Singleton;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.TenantFeature;
import io.quarkus.oidc.common.ClientAttester;
import io.quarkus.oidc.common.runtime.OidcConstants;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;

@Singleton
@Unremovable
@TenantFeature("tenant-attestation-custom")
public class CustomClientAttester implements ClientAttester {

    static volatile boolean attestationJwtCreated;

    @Override
    public Uni<String> attest(ClientAttestationContext context) {
        if (context.clientPublicKeyJwk() == null) {
            return Uni.createFrom().failure(
                    new RuntimeException(
                            "No client public key is available for client: " + context.clientId()));
        }
        AttestationSigningKey signingKey = context.attestationSigningKey();
        if (signingKey == null) {
            return Uni.createFrom().failure(
                    new RuntimeException(
                            "No attestation signing key is available for client: " + context.clientId()));
        }
        JsonObject cnf = new JsonObject().put("jwk", context.clientPublicKeyJwk().getMap());
        String jwt = Jwt.issuer(context.clientId())
                .subject(context.clientId())
                .claim("cnf", cnf.getMap())
                .claim("custom_attester", true)
                .expiresIn(signingKey.lifespan())
                .jws()
                .type(OidcConstants.CLIENT_ATTESTATION_JWT_TYPE)
                .algorithm(signingKey.signatureAlgorithm())
                .keyId(signingKey.kid())
                .sign(signingKey.signingKey());
        attestationJwtCreated = true;
        return Uni.createFrom().item(jwt);
    }

    @Override
    public boolean selfAttesting() {
        return true;
    }
}
