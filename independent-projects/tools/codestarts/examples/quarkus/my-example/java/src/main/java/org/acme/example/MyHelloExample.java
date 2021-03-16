package org.acme.example;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/my-hello-example")
public class MyHelloExample {

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String hello() {
        return "My Example Hello Quarkus Codestart";
    }

}
