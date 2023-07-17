package io.quarkus.it.amazon.lambda;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;

@Path("/hello")
public class GreetingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "hello";
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public String hello(String name) {
        return "hello " + name;
    }

    @POST
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] hello(byte[] bytes) {
        if (bytes[0] != 0 || bytes[1] != 1 || bytes[2] != 2 || bytes[3] != 3) {
            throw new RuntimeException("bad input");
        }
        byte[] rtn = { 4, 5, 6 };
        return rtn;
    }

    @POST
    @Path("empty")
    public void empty() {

    }

    @Context
    com.amazonaws.services.lambda.runtime.Context ctx;

    @GET
    @Path("context")
    @Produces(MediaType.TEXT_PLAIN)
    public void context() {
        if (ctx == null)
            throw new RuntimeException();
        if (ctx.getAwsRequestId() == null)
            throw new RuntimeException("aws context not set");
    }

    @GET
    @Path("proxyRequestContext")
    @Produces(MediaType.TEXT_PLAIN)
    public void proxyRequestContext(@Context APIGatewayV2HTTPEvent event) {
        if (event == null)
            throw new RuntimeException();
    }

    @Inject
    APIGatewayV2HTTPEvent injectEvent;

    @GET
    @Path("inject-event")
    @Produces(MediaType.TEXT_PLAIN)
    public void injectEvent() {
        if (injectEvent == null)
            throw new RuntimeException();
    }

}
