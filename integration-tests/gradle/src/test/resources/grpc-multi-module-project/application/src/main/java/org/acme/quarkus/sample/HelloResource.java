package org.acme.quarkus.sample;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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