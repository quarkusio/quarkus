package io.quarkus.resteasy.test;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/post")
public class PostEndpoint {

    public static volatile boolean invoked = false;

    @GET
    public String get() {
        return "ok";
    }

    @POST
    public void post(byte[] data) {
        invoked = true;
    }
}
