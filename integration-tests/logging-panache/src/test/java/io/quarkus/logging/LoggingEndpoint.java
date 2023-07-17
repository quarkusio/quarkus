package io.quarkus.logging;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/logging")
public class LoggingEndpoint {
    @GET
    public String hello() {
        Log.info("hello");
        return "hello";
    }
}
