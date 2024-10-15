package io.quarkus.it.opentelemetry;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;

public class PathTemplateResource {

    @GET
    public String get(@PathParam("value") String value) {
        return "Received: " + value;
    }
}
