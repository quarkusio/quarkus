package io.quarkus.micrometer.test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/greeting")
@ApplicationScoped
public class GreetingResource {

    @RegisterRestClient(configKey = "greeting")
    public interface GreetingRestClient {
        @GET
        @Path("/echo/{message}")
        @Consumes(MediaType.TEXT_PLAIN)
        String echo(@PathParam("message") String name);
    }

    @RestClient
    GreetingRestClient greetingRestClient;

    @GET
    @Path("/{message}")
    public String passThrough(@PathParam("message") String message) {
        return greetingRestClient.echo(message + " World!");
    }

    @GET
    @Path("/echo/{message}")
    public Response echo(@PathParam("message") String message) {
        return Response.ok(message, "text/plain").build();
    }

}
