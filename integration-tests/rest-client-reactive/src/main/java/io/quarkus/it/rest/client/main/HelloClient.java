package io.quarkus.it.rest.client.main;

import javax.ws.rs.Consumes;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.rest.client.reactive.ClientExceptionMapper;

@Path("")
public interface HelloClient {
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    String greeting(String name, @QueryParam("count") int count);

    // this isn't used, but it makes sure that the generated provider can be properly instantiated in native mode
    @ClientExceptionMapper
    static RuntimeException toException(Response response) {
        if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
            return new NotFoundException("not found");
        }
        return null;
    }
}
