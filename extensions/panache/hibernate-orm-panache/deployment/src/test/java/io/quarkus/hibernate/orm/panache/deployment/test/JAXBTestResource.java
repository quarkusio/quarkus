package io.quarkus.hibernate.orm.panache.deployment.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.Assertions;

@Path("test")
public class JAXBTestResource {

    @Produces(MediaType.APPLICATION_XML)
    @GET
    @Path("ignored-properties")
    public JAXBEntity ignoredProperties() throws NoSuchMethodException, SecurityException {
        JAXBEntity.class.getMethod("$$_hibernate_read_id");
        JAXBEntity.class.getMethod("$$_hibernate_read_name");
        try {
            JAXBEntity.class.getMethod("$$_hibernate_read_persistent");
            Assertions.fail();
        } catch (NoSuchMethodException e) {
        }

        // no need to persist it, we can fake it
        JAXBEntity entity = new JAXBEntity();
        entity.id = 666l;
        entity.name = "Eddie";
        return entity;
    }
}
