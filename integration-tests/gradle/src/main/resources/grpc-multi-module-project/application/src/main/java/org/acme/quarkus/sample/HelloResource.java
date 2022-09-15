package org.acme.quarkus.sample;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import devmodetest.v1.Devmodetest;

import org.acme.common.CommonBean;

@Path("/hello")
public class HelloResource {

    @Inject
    CommonBean common;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        Integer number = Devmodetest.DevModeResponse.Status.TEST_ONE.getNumber();
        // return a thing from proto file (for devmode test)
        return "hello " + number;
    }
}
