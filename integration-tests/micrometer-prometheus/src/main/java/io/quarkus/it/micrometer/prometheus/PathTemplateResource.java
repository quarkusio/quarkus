package io.quarkus.it.micrometer.prometheus;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import io.smallrye.common.vertx.ContextLocals;

@Path("template/path/{value}")
public class PathTemplateResource {
    @GET
    public String get(@PathParam("value") String value) {
        ContextLocals.put("context-local", "val-" + value);
        return "Received: " + value;
    }
}
