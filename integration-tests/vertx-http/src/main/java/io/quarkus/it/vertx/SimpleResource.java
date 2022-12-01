package io.quarkus.it.vertx;

import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

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
