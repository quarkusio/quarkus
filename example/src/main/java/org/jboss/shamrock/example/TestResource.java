package org.jboss.shamrock.example;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/test")
public class TestResource {

    @GET
    public String getTest() {
        return "TEST";
    }

}
