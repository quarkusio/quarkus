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

}
