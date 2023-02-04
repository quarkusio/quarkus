package io.quarkus.restclient.registerclientheaders;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/echo")
public class EchoResource {

    @RestClient
    EchoClient client;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public String echo(@QueryParam("message") String message, @HeaderParam("foo") String foo) {
        return message + foo;
    }

}
