package io.quarkus.it.keycloak;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.oidc.token.propagation.AccessToken;

@RegisterRestClient(configKey = "access-token-propagation")
@AccessToken
@Path("/")
public interface AccessTokenPropagationService {

    @GET
    String getUserName();
}
