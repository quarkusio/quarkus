package org.acme;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/containsExtB")
public class HasExtBResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String containsExtB() {
        return Boolean.toString(hasExtB());
    }

    public boolean hasExtB() {
        return Thread.currentThread().getContextClassLoader().getResource("org/acme/B.properties") != null;
    }
}
