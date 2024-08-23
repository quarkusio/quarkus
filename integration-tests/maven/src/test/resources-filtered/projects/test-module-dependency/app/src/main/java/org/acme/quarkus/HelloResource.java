package org.acme.quarkus;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class HelloResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() throws ClassNotFoundException {
        try {
            Class.forName("org.acme.SomeTestSupport");
            return "Test class loaded";
        } catch (ClassNotFoundException e) {
            // expected
        }
        Class.forName("org.acme.AcmeUtil");
        Class.forName("org.acme.AcmeCommon");
        return "Test class is not visible";
    }
}
