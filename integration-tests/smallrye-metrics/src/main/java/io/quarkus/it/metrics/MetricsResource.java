package io.quarkus.it.metrics;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.ConcurrentGauge;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Metric;
import org.eclipse.microprofile.metrics.annotation.SimplyTimed;
import org.eclipse.microprofile.metrics.annotation.Timed;

@Path("/metricsresource")
public class MetricsResource {

    @Inject
    @Metric(name = "histogram")
    Histogram histogram;

    @GET
    @Path("/counter")
    @Counted(name = "a_counted_resource")
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
    @Path("/simpletimer")
    @SimplyTimed(name = "simple_timer_metric")
    public String simpleTimer() {
        return "OK";
    }

    @GET
    @Path("/timer")
    @Timed(name = "timer_metric")
    public String timer() {
        return "OK";
    }

    // used to control the invocation of /cgauge - it will not finish until instructed by an invocation to /cgauge_finish
    private volatile CountDownLatch latch = new CountDownLatch(1);

    @GET
    @Path("/cgauge")
    public String cgauge() {
        new Thread(this::cGaugedMethod).start();
        return "OK";
    }

    @ConcurrentGauge(name = "cgauge")
    String cGaugedMethod() {
        try {
            latch.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "OK";
    }

    @GET
    @Path("/cgauge_finish")
    public String cgaugeFinish() {
        latch.countDown();
        latch = new CountDownLatch(1);
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
    @Counted(name = "counter_absolute", absolute = true)
    public String counterAbsoluteName() {
        return "TEST";
    }

    @GET
    @Path("/counter-with-tags")
    @Counted(name = "counter_with_tags", tags = "foo=bar")
    public String counterWithTags() {
        return "TEST";
    }

    @GET
    @Path("/counter-throwing-not-found-exception")
    @Counted(name = "counter_404")
    public String counterWithTagsThrowingNotFound() {
        throw new NotFoundException();
    }

}
