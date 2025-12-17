package io.quarkus.it.jaxb;

import java.io.StringReader;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBContext;

import io.quarkus.it.jaxb.Book.Cover;

@Path("/book")
@ApplicationScoped
public class Resource {
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response postBook(Book book) throws Exception {
        JAXBContext jaxbContext = JAXBContext.newInstance(Book.Cover.class);
        Book.Cover cover = (Cover) jaxbContext.createUnmarshaller()
                .unmarshal(new StringReader("<cover><image>" + book.getCover() + "</image></cover>"));
        return Response.ok(cover.getImage().getHeight(null)).build();
    }
}
