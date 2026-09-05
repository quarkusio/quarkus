package io.quarkus.it.observation.reactive;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;

@RegisterRestClient(configKey = "client")
@Path("/reactive")
public interface ReactiveRestClient {

    @GET
    Uni<String> helloGet(@QueryParam("name") String name);
}
