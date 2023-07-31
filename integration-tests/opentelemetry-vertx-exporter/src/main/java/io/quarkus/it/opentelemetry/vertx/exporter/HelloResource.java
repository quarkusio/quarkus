package io.quarkus.it.opentelemetry.vertx.exporter;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("hello")
public class HelloResource {

    @GET
    public String get() {
        return "get";
    }

}
