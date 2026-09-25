package io.quarkus.oidc.token.propagation.graphql;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.security.Authenticated;

@Path("/frontend")
@Authenticated
public class FrontendResource {

    @Inject
    PropagationTypesafeGraphQLClient client;

    @GET
    @Path("/me")
    public String me() {
        return client.me();
    }
}
