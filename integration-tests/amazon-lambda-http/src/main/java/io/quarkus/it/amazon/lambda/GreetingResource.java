package io.quarkus.it.amazon.lambda;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
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
    @Path("comma")
    public String comma(@HeaderParam("Access-Control-Request-Headers") String access) {
        if (access == null || !access.contains(","))
            throw new RuntimeException("should have comma");
        return "ok";
    }

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

    @GET
    @Path("context")
    @Produces(MediaType.TEXT_PLAIN)
    public void context(@Context com.amazonaws.services.lambda.runtime.Context ctx) {
        if (ctx == null)
            throw new RuntimeException();
        if (ctx.getAwsRequestId() == null)
            throw new RuntimeException("aws context not set");
    }

    @GET
    @Path("proxyRequestContext")
    @Produces(MediaType.TEXT_PLAIN)
    public void proxyRequestContext(@Context APIGatewayV2HTTPEvent ctx) {
        if (ctx == null || ctx.getRequestContext().getHttp().getMethod() == null)
            throw new RuntimeException();
    }

    @Inject
    APIGatewayV2HTTPEvent event;

    @GET
    @Path("inject-event")
    @Produces(MediaType.TEXT_PLAIN)
    public void injectEvent() {
        if (event == null)
            throw new RuntimeException();
    }

}
