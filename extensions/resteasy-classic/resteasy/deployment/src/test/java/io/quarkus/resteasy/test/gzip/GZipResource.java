package io.quarkus.resteasy.test.gzip;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.GZIP;

@Path("gzip")
public class GZipResource {

    public static final String BODY = "hello world this string is going to be compressed";

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @GZIP
    public String hello() {
        return BODY;
    }

}
