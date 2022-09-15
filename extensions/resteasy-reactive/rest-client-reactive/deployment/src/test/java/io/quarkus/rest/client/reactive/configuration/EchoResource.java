package io.quarkus.rest.client.reactive.configuration;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;

@Path("/hello")
@Produces(MediaType.TEXT_PLAIN)
@Consumes(MediaType.TEXT_PLAIN)
public class EchoResource {
    @POST
    public String echo(String name, @Context Request request, @Context HttpHeaders httpHeaders) {
        var message = httpHeaders.getHeaderString("message");
        var comma = httpHeaders.getHeaderString("comma");
        var suffix = httpHeaders.getHeaderString("suffix");
        return (message != null ? message : "hello") + (comma != null ? comma : "_") + " " + name
                + (suffix != null ? suffix : "");
    }
}
