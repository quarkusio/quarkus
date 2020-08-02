package io.quarkus.it.rest.client.wronghost;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
public interface WrongHostClient {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    String root();
}
