package io.quarkus.resteasy.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

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
