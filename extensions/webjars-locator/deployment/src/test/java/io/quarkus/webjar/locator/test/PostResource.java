package io.quarkus.webjar.locator.test;

import jakarta.annotation.PreDestroy;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

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
