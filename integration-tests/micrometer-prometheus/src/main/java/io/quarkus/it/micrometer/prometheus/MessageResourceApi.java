package io.quarkus.it.micrometer.prometheus;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

// Splitting JAX-RS annotations to interfaces tests the behaviour of
// io.quarkus.resteasy.deployment.RestPathAnnotationProcessor
public interface MessageResourceApi extends MessageResourceNestedApi {

    @GET
    @Path("match/{id}/{sub}")
    String match(@PathParam("id") String id, @PathParam("sub") String sub);
}
