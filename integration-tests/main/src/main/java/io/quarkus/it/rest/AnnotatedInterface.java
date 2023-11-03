package io.quarkus.it.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/interface")
public interface AnnotatedInterface {

    @GET
    String get();

}
