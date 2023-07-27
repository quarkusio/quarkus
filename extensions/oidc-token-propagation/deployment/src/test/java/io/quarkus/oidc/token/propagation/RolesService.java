package io.quarkus.oidc.token.propagation;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "roles")
@AccessToken
@Path("/")
public interface RolesService {

    @GET
    String getRole();
}
