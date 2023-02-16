package io.quarkus.it.micrometer.prometheus;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

// Testing deep lookup of Path-annotation
public interface MessageResourceNestedApi {

    @GET
    @Path("match/{text}")
    String optional(@PathParam("text") String text);
}
