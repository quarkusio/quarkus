package io.quarkus.it.jaxb;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;

@Path("/jaxb")
@ApplicationScoped
public class Resource {

    private static final Logger LOGGER = Logger.getLogger(Resource.class);

    @Path("/book")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.TEXT_PLAIN)
    public Response postBook(Book book) {
        LOGGER.info("Received book: " + book);
        try {
            return Response.accepted().entity(book.getCover().getHeight(null)).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

}
