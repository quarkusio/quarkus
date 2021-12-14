package io.quarkus.it.jaxb;

import java.io.StringWriter;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.jboss.logging.Logger;

@Path("/jaxb")
@ApplicationScoped
public class JaxbResource {

    private static final Logger LOGGER = Logger.getLogger(JaxbResource.class);

    @Path("/book")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getBook(@QueryParam("name") String name) throws JAXBException {
        Book book = new Book();
        book.setTitle(name);
        JAXBContext context = JAXBContext.newInstance(book.getClass());
        Marshaller marshaller = context.createMarshaller();
        StringWriter sw = new StringWriter();
        marshaller.marshal(book, sw);
        return sw.toString();
    }

    @Path("/book")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.TEXT_PLAIN)
    public Response postBook(Book book) {
        try {
            if (book.getCover() == null) {
                return Response
                        .accepted()
                        .entity("No Cover").build();
            }
            return Response
                    .accepted()
                    .entity(book.getCover().getHeight(null)).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.serverError().entity(e.getMessage()).build();
        }
    }
}
