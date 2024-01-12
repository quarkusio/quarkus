package io.quarkus.rest.client.reactive.stork;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/")
@RegisterRestClient(configKey = "hello2")
public interface HelloClient {
    @GET
    String hello();

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Path("/")
    String echo(String name);

    @GET
    @Path("/{name}")
    public String helloWithPathParam(@PathParam("name") String name);
}
