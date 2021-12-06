package io.quarkus.it.jaxb;

import java.io.StringWriter;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

@Path("/jaxb")
@ApplicationScoped
public class JaxbResource {

    @Path("/book")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getBook(@QueryParam("name") String name)
            throws JAXBException {
        Book book = new Book();
        book.setTitle(name);
        JAXBContext context = JAXBContext.newInstance(book.getClass());
        Marshaller marshaller = context.createMarshaller();
        StringWriter sw = new StringWriter();
        marshaller.marshal(book, sw);
        return sw.toString();
    }

}
