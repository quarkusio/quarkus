package org.jboss.shamrock.example.opentracing;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.opentracing.Traced;

@Path("/opentracing")
public class OpenTracingResource {

    @GET
    @Traced
    public String getTest() {
        return "TEST";
    }


}
