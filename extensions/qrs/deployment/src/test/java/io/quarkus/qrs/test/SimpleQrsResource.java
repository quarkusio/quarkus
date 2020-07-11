package io.quarkus.qrs.test;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/simple")
public class SimpleQrsResource {

    @GET
    public String get() {
        return "GET";
    }

    @GET
    @Path("{id}")
    public String get(String id) {
        return "GET:" + id;
    }

    @POST
    public String post() {
        return "POST";
    }
}
