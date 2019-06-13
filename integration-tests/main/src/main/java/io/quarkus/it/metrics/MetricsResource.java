package io.quarkus.it.metrics;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;

import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Metric;
import org.eclipse.microprofile.metrics.annotation.Timed;

@Path("/metricsresource")
public class MetricsResource {

    @Inject
    @Metric(name = "histogram")
    Histogram histogram;

    @GET
    @Path("/counter")
    @Counted(monotonic = true, name = "a_counted_resource")
    public String counter() {
        return "TEST";
    }

    @GET
    @Path("/gauge")
    @Gauge(name = "gauge", unit = MetricUnits.NONE)
    public Long gauge() {
        return 42L;
    }

    @GET
    @Path("/meter")
    @Metered(name = "meter")
    public String meter() {
        return "OK";
    }

    @GET
    @Path("/timer")
    @Timed(name = "timer_metric")
    public String timer() {
        return "OK";
    }

    @GET
    @Path("/histogram")
    public String histogram() {
        histogram.update(42);
        return "OK";
    }

    @GET
    @Path("/counter-absolute")
    @Counted(monotonic = true, name = "counter_absolute", absolute = true)
    public String counterAbsoluteName() {
        return "TEST";
    }

    @GET
    @Path("/counter-with-tags")
    @Counted(monotonic = true, name = "counter_with_tags", tags = "foo=bar")
    public String counterWithTags() {
        return "TEST";
    }

    @GET
    @Path("/counter-throwing-not-found-exception")
    @Counted(monotonic = true, name = "counter_404")
    public String counterWithTagsThrowingNotFound() {
        throw new NotFoundException();
    }

}
