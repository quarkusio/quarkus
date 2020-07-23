package io.quarkus.qrs.test;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.qrs.Blocking;
import io.quarkus.qrs.runtime.core.RequestContext;
import io.quarkus.runtime.BlockingOperationControl;
import io.vertx.core.buffer.Buffer;

@Path("/simple")
public class SimpleQrsResource {

    @Inject
    HelloService service;

    @GET
    public String get() {
        return "GET";
    }

    @GET
    @Path("/hello")
    public String hello() {
        return service.sayHello();
    }

    @GET
    @Path("{id}")
    public String get(@PathParam("id") String id) {
        return "GET:" + id;
    }

    @POST
    @Path("params/{p}")
    public String params(@PathParam("p") String p,
            @QueryParam("q") String q,
            @HeaderParam("h") String h,
            @FormParam("f") String f) {
        return "params: p: " + p + ", q: " + q + ", h: " + h + ", f: " + f;
    }

    @POST
    public String post() {
        return "POST";
    }

    @DELETE
    public String delete() {
        return "DELETE";
    }

    @PUT
    public String put() {
        return "PUT";
    }

    @PATCH
    public String patch() {
        return "PATCH";
    }

    @OPTIONS
    public String options() {
        return "OPTIONS";
    }

    @HEAD
    public Response head() {
        return Response.ok().header("Stef", "head").build();
    }

    @GET
    @Path("/person")
    @Produces(MediaType.APPLICATION_JSON)
    public Person getPerson() {
        Person person = new Person();
        person.setFirst("Bob");
        person.setLast("Builder");
        return person;
    }

    @GET
    @Path("/blocking")
    @Blocking
    public String blocking() {
        return String.valueOf(BlockingOperationControl.isBlockingAllowed());
    }

    @GET
    @Path("filters")
    public Response filters(@Context HttpHeaders headers) {
        return Response.ok().header("filter", headers.getHeaderString("filter")).build();
    }

    @GET
    @Path("mapped-exception")
    public String mappedException() {
        throw new TestException();
    }

    @GET
    @Path("unknown-exception")
    public String unknownException() {
        throw new RuntimeException("OUCH");
    }

    @GET
    @Path("web-application-exception")
    public String webApplicationException() {
        throw new WebApplicationException(Response.status(666).entity("OK").build());
    }

    @GET
    @Path("writer")
    public TestClass writer() {
        return new TestClass();
    }

    @GET
    @Path("fast-writer")
    public String fastWriter(@Context RequestContext context) {
        return context.getTarget().getBuildTimeWriter() != null ? "OK" : "FAIL";
    }

    @GET
    @Path("lookup-writer")
    public Object slowWriter(@Context RequestContext context) {
        return context.getTarget().getBuildTimeWriter() == null ? "OK" : "FAIL";
    }

    @GET
    @Path("writer/vertx-buffer")
    public Buffer vertxBuffer(@Context RequestContext context) {
        return Buffer.buffer(context.getTarget().getBuildTimeWriter() != null ? "VERTX-BUFFER" : "FAIL");
    }
}
