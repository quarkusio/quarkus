package io.quarkus.it.micrometer.prometheus;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("template/path/{value}")
public class PathTemplateResource {
    @GET
    public String get(@PathParam("value") String value) {
        return "Received: " + value;
    }
}
