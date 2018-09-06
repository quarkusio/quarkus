package org.jboss.shamrock.example.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/foo")
public interface RestInterface {

    @GET
    void get();

}
