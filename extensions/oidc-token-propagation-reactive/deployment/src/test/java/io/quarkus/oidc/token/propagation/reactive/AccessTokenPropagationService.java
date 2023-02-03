package io.quarkus.oidc.token.propagation.reactive;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.oidc.token.propagation.AccessToken;

@RegisterRestClient
@AccessToken
@Path("/")
public interface AccessTokenPropagationService {

    @GET
    String getUserName();
}
