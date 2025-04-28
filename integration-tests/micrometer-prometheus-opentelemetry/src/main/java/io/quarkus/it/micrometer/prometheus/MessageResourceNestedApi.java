package io.quarkus.it.micrometer.prometheus;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

// Testing deep lookup of Path-annotation
public interface MessageResourceNestedApi {

    @GET
    @Path("match/{text}")
    String optional(@PathParam("text") String text);
}