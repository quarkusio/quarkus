package io.quarkus.it.opentelemetry;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("template/path/{value}")
public class PathTemplateResource {
    @GET
    public String get(@PathParam("value") String value) {
        return "Received: " + value;
    }
}
