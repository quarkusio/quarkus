package io.quarkus.it.external.exporter;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("hello")
@Produces(MediaType.APPLICATION_JSON)
public class HelloResource {
    private static final Logger LOG = LoggerFactory.getLogger(HelloResource.class);

    @GET
    public String get() {
        LOG.info("Hello World");
        return "get";
    }
}
