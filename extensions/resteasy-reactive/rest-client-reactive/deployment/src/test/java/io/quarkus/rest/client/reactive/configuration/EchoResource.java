package io.quarkus.rest.client.reactive.configuration;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;

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
