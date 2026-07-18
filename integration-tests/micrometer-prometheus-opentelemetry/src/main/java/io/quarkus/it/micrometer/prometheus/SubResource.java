package io.quarkus.it.micrometer.prometheus;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

public class SubResource {

    private final String value;

    public SubResource(String value) {
        this.value = value;
    }

    @GET
    @Path("/{subParam}")
    public String get(@PathParam("subParam") String subParam) {
        return value + ":" + subParam;
    }
}
