package io.quarkus.it.jaxb;

import java.io.StringWriter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

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

    @GET
    @Path("/see-also")
    @Produces(MediaType.APPLICATION_XML)
    public io.quarkus.it.jaxb.Response seeAlso() {
        io.quarkus.it.jaxb.Response response = new io.quarkus.it.jaxb.Response();
        response.setEvenMoreZeep("ZEEEP");
        return response;
    }

}
