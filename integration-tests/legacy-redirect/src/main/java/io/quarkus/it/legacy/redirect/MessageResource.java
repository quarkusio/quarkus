package io.quarkus.it.legacy.redirect;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/message")
public class MessageResource {

    @GET
    public String message() {
        return "hello world";
    }

}
