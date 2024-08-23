package io.quarkus.it.keycloak;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.oidc.token.propagation.JsonWebToken;

@RegisterRestClient
@JsonWebToken
@Path("/")
public interface JwtTokenPropagationService {

    @GET
    String getUserName();
}
