package io.quarkus.it.resteasy;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path("/context")
public class ContextResource {

    @GET
    @Path("/servletcontext")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean servletContextInjected(@Context ServletContext servletContext) {
        return servletContext != null;
    }
}
