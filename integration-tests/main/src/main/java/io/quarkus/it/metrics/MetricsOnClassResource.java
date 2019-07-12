package io.quarkus.it.metrics;

import javax.ws.rs.Path;

import org.eclipse.microprofile.metrics.annotation.Counted;

@Path("/metricsonclass")
@Counted(name = "foo", absolute = true)
public class MetricsOnClassResource {

    @Path("/method")
    public void method() {

    }

}
