package org.jboss.shamrock.example.metrics;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.metrics.annotation.Counted;

@Path("/metrics")
public class MetricsResource {

    @GET
    @Counted(monotonic = true, name = "a_counted_resource")
    public String getTest() {
        return "TEST";
    }


}
