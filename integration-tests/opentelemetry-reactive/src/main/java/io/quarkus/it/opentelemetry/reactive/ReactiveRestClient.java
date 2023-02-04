package io.quarkus.it.opentelemetry.reactive;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;

@RegisterRestClient(configKey = "client")
@Path("/reactive")
interface ReactiveRestClient {
    @GET
    Uni<String> helloGet(@QueryParam("name") String name);

    @POST
    Uni<String> helloPost(String body);
}
