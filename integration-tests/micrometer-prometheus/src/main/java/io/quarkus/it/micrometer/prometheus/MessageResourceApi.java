package io.quarkus.it.micrometer.prometheus;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

// Splitting JAX-RS annotations to interfaces tests the behaviour of
// io.quarkus.resteasy.deployment.RestPathAnnotationProcessor
public interface MessageResourceApi extends MessageResourceNestedApi {

    @GET
    @Path("match/{id}/{sub}")
    String match(@PathParam("id") String id, @PathParam("sub") String sub);
}
