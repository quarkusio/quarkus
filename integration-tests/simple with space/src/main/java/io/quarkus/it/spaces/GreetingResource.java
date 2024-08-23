package io.quarkus.it.spaces;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.primefaces.util.Constants;

@Path("/hello")
public class GreetingResource {

    // make sure we reference something from Primefaces so GraalVM doesn't throw out the entire jar
    private static final String PRIMEFACES_DOWNLOAD_COOKIE = Constants.DOWNLOAD_COOKIE;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello";
    }
}
