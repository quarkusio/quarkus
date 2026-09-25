package io.quarkus.opentelemetry.deployment.devui;

import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;

@Path("/otel-metrics-test")
@ApplicationScoped
public class CustomMetricResource {

    private final LongCounter counter;
    private final AtomicLong gaugeValue = new AtomicLong();

    @Inject
    public CustomMetricResource(OpenTelemetry openTelemetry) {
        Meter meter = openTelemetry.getMeter("dev-ui-test");
        this.counter = meter.counterBuilder("custom.otel.hits").build();
        // Observable instrument: exercises the async-callback path and the
        // "collected twice in dev" behavior (dev reader + OTLP reader).
        meter.gaugeBuilder("custom.otel.gauge")
                .buildWithCallback(m -> m.record(gaugeValue.get()));
    }

    @GET
    @Path("/hit")
    public String hit() {
        counter.add(1);
        gaugeValue.incrementAndGet();
        return "ok";
    }
}
