package org.acme.quarkus.sample;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.acme.quarkus.sample.extension.ConfigReport;

@Path("/hello")
public class HelloResource {

    @Inject
    ConfigReport configReport;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "builder-image is " + configReport.builderImage;
    }
}