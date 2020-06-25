package org.acme.quarkus.sample;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import examples.MutinyGreeterGrpc;

import org.acme.common.CommonBean;

@Path("/hello")
public class HelloResource {

    @Inject
    CommonBean common;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        // just to check if the class is available
        MutinyGreeterGrpc.MutinyGreeterStub stub;
        return "hello " + common.getName();
    }
}