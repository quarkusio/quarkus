package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.concurrent.atomic.LongAdder;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.quarkus.micrometer.runtime.meters.Gauges;

public class VertxTcpServerMetrics extends NetworkMetrics {

    VertxTcpServerMetrics(MeterRegistry registry, String prefix, Tags tags, Gauges<LongAdder> gauges) {
        super(registry, tags, prefix,
                "Number of bytes received by the server",
                "Number of bytes sent by the server",
                "The duration of the connections",
                "Number of opened connections to the HTTP Server",
                gauges);
    }

}
