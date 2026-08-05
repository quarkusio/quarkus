package io.quarkus.micrometer.runtime.binder.vertx;

import java.util.concurrent.atomic.LongAdder;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.quarkus.micrometer.runtime.meters.Gauges;

public class VertxTcpClientMetrics extends NetworkMetrics {

    VertxTcpClientMetrics(MeterRegistry registry, String prefix, Tags tags, Gauges<LongAdder> gauges) {
        super(registry, tags, prefix,
                "Number of bytes received by the client",
                "Number of bytes sent by the client",
                "The duration of the connections",
                "Number of connections to the remote host currently opened",
                gauges);
    }

}
