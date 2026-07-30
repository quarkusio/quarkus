package io.quarkus.oidc.token.propagation.reactive.deployment.test;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.oidc.token.propagation.common.AccessToken;

@RegisterRestClient
@AccessToken
@Path("/")
@Consumes("text/plain")
public interface AccessTokenPropagationService {

    @GET
    String getUserName();

    @POST
    @Consumes("text/plain")
    String echoUserName(String name);
}
