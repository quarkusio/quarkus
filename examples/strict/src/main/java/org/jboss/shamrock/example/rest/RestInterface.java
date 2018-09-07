package org.jboss.shamrock.example.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/test")
public interface RestInterface {

    @GET
    String get();

}
