package io.quarkus.it.vertx;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

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
