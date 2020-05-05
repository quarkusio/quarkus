package io.quarkus.it.spaces;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
