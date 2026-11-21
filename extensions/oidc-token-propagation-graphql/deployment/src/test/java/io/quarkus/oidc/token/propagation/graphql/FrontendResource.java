package io.quarkus.oidc.token.propagation.graphql;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;

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

    @GET
    @Path("/me-async")
    public Uni<String> meAsync() {
        return client.meAsync();
    }
}
