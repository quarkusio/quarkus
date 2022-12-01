package io.quarkus.it.keycloak;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.oidc.token.propagation.JsonWebToken;

@RegisterRestClient
@JsonWebToken
@Path("/")
public interface JwtTokenPropagationService {

    @GET
    String getUserName();
}
