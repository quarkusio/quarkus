package io.quarkus.it.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/interface")
public interface AnnotatedInterface {

    @GET
    String get();

}
