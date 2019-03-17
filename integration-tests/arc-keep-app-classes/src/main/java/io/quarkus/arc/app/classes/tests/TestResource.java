package io.quarkus.arc.app.classes.tests;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/classes")
public class TestResource {

    @Inject
    BeanManager beanManager;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int numberOfServices() {
        return beanManager.getBeans(SomeService.class).size();
    }
}
