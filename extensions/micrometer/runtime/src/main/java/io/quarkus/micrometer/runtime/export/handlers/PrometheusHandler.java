package io.quarkus.micrometer.runtime.export.handlers;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class PrometheusHandler implements Handler<RoutingContext> {
    private static final Logger log = Logger.getLogger(PrometheusHandler.class);

    private PrometheusMeterRegistry registry;

    private boolean setup = false;

    @Override
    public void handle(RoutingContext routingContext) {
        if (!setup) {
            setup();
        }

        HttpServerResponse response = routingContext.response();
        if (registry == null) {
            response.setStatusCode(500)
                    .setStatusMessage("Unable to resolve Prometheus registry instance");
        } else {
            response.putHeader("Content-Type", TextFormat.CONTENT_TYPE_004)
                    .end(Buffer.buffer(registry.scrape()));
        }
    }

    private void setup() {
        Instance<PrometheusMeterRegistry> registries = CDI.current().select(PrometheusMeterRegistry.class,
                Default.Literal.INSTANCE);

        if (registries.isUnsatisfied()) {
            registry = null;
        } else if (registries.isAmbiguous()) {
            registry = registries.iterator().next();
            log.warnf("Multiple prometheus registries present. Using %s with the built-in scrape endpoint",
                    registry);
        } else {
            registry = registries.get();
        }

        setup = true;
    }
}
