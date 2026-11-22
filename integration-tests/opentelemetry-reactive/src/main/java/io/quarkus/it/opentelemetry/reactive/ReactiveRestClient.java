package io.quarkus.it.opentelemetry.reactive;

import java.io.InputStream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;

@RegisterRestClient(configKey = "client")
@Path("/reactive")
interface ReactiveRestClient {
    @GET
    Uni<String> helloGet(@QueryParam("name") String name);

    @POST
    Uni<String> helloPost(String body);

    @POST
    @Path("/stream")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    Uni<byte[]> stream(InputStream body);

    @GET
    @Path("/hello-get-uni-executor")
    Uni<String> helloGetUniExecutor();

    @GET
    @Path("/hello-get-uni-delay")
    Uni<String> helloGetUniDelay();
}
