package io.quarkus.resteasy.test.gzip;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
