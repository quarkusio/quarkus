package io.quarkus.it.vertx;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/filter")
public class FilterResource {

    @GET
    @Path("/any")
    public String headers() {
        return "ok";
    }

    @GET
    @Path("/another")
    public String another() {
        return "ok";
    }

    @GET
    @Path("/override")
    public Response headersOverride() {
        return Response.ok("ok").header("Cache-Control", "max-age=0").build();
    }

    @GET
    @Path("/no-cache")
    public String noCache() {
        return "ok";
    }

    @GET
    @Path("/order")
    public String order() {
        return "ok";
    }

    @GET
    @Path("/apply-on-success/ok")
    public String applyOnSuccessOk() {
        return "ok";
    }

    @GET
    @Path("/apply-on-success/no-content")
    public Response applyOnSuccessNoContent() {
        return Response.noContent().build();
    }

    @GET
    @Path("/apply-on-success/custom-in-range")
    public Response applyOnSuccessCustomInRange() {
        return Response.status(267).entity("custom-success").build();
    }

    @GET
    @Path("/apply-on-success/error")
    public Response applyOnSuccessError() {
        return Response.status(403).entity("forbidden").build();
    }

    @GET
    @Path("/apply-on-success/server-error")
    public Response applyOnSuccessServerError() {
        return Response.status(500).entity("error").build();
    }

}
