package io.quarkus.oidc.token.propagation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@AccessToken
@Path("/")
public interface AccessTokenPropagationService {

    @GET
    String getUserName();
}
