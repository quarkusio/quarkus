package io.quarkus.oidc.token.propagation.reactive.deployment.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.oidc.token.propagation.common.AccessToken;
import io.smallrye.mutiny.Uni;

@RegisterRestClient(configKey = "roles")
@AccessToken
@Path("/")
public interface RolesService {

    @GET
    Uni<String> getRole();
}
