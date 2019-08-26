package io.quarkus.resteasy.test;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/post")
public class PostResource {

    @POST
    public String modify(String data) {
        return "Hello: " + data;
    }
}
