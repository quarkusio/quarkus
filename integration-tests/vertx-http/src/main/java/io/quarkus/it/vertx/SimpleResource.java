package io.quarkus.it.vertx;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

/**
 *
 */
@Path("/simple")
public class SimpleResource {
    @GET
    @Path("/access-log-test-endpoint")
    public String accessLogTest() {
        return "passed";
    }

    @OPTIONS
    @Path("/options")
    public Response optionsHandler() {
        return Response.ok("options").header("X-Custom-Header", "abc").build();
    }
}
