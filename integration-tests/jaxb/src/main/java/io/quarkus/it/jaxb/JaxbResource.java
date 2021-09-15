package io.quarkus.it.jaxb;

import java.io.ByteArrayOutputStream;
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

import io.quarkus.it.jaxb.domain.Customer;

@Path("/jaxb")
@ApplicationScoped
public class JaxbResource {

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

    @Path("/customer")
    @Produces(MediaType.APPLICATION_XML)
    @GET
    public String getCustomer() throws JAXBException {
        Customer customer = new Customer();
        customer.setName("fake-name");
        customer.setAge(18);
        customer.setId(1);

        JAXBContext context = JAXBContext.newInstance("io.quarkus.it.jaxb.domain");

        Marshaller mar = context.createMarshaller();
        // output pretty printed
        mar.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mar.marshal(customer, out);

        return out.toString();
    }

}
