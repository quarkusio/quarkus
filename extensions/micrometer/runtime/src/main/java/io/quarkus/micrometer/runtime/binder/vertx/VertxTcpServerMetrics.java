package io.quarkus.micrometer.runtime.binder.vertx;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

public class VertxTcpServerMetrics extends NetworkMetrics {

    VertxTcpServerMetrics(MeterRegistry registry, String prefix, Tags tags) {
        super(registry, tags, prefix,
                "Number of bytes received by the server",
                "Number of bytes sent by the server",
                "The duration of the connections",
                "Number of opened connections to the HTTP Server");
    }

}
