package io.quarkus.resteasy.test;

import javax.annotation.PreDestroy;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/post")
public class PostResource {

    @POST
    public String modify(String data) {
        return "Hello: " + data;
    }

    @PreDestroy
    void destroy() {
        throw new IllegalStateException("Something bad happened but dev mode should work fine");
    }

}
