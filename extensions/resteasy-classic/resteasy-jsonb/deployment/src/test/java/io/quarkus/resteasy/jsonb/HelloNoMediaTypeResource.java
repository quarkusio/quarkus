package io.quarkus.resteasy.jsonb;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/hello-default")
public class HelloNoMediaTypeResource {

    @GET
    public Message hello() {
        Message m = new Message();
        m.setMessage("Hello");
        return m;
    }
}
