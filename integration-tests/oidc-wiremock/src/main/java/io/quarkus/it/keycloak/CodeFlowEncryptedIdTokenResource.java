package io.quarkus.it.keycloak;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.oidc.IdToken;
import io.quarkus.security.Authenticated;

@Path("/code-flow-encrypted-id-token")
public class CodeFlowEncryptedIdTokenResource {

    @Inject
    @IdToken
    JsonWebToken idToken;

    @GET
    @Authenticated
    @Path("/code-flow-encrypted-id-token-jwk")
    public String accessJwk() {
        return "user: " + idToken.getName();
    }

    @GET
    @Authenticated
    @Path("/code-flow-encrypted-id-token-pem")
    public String accessPem() {
        return "user: " + idToken.getName();
    }
}
