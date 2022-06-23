package io.quarkus.it.opentelemetry.reactive;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

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
