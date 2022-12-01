package io.quarkus.logging;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/logging")
public class LoggingEndpoint {
    @GET
    public String hello() {
        Log.info("hello");
        return "hello";
    }
}
