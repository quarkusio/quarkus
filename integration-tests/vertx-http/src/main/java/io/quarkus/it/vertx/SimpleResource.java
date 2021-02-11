package io.quarkus.it.vertx;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
}
