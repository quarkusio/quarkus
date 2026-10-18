package io.quarkus.resteasy.jsonb;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/hello-default")
public class HelloNoMediaTypeResource {

    @GET
    public Message hello() {
        Message m = new Message();
        m.setMessage("Hello");
        return m;
    }
}
