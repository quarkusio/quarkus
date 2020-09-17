package io.quarkus.restclient.registerclientheaders;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

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
