package io.quarkus.micrometer.runtime.export.handlers;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class PrometheusHandler implements Handler<RoutingContext> {
    // see io.micrometer.prometheusmetrics.PrometheusMeterRegistry.Format
    public final static String CONTENT_TYPE_004 = "text/plain; version=0.0.4; charset=utf-8";
    public final static String CONTENT_TYPE_OPENMETRICS_100 = "application/openmetrics-text; version=1.0.0; charset=utf-8";

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
            ManagedContext requestContext = Arc.container().requestContext();
            var acceptHeader = chooseContentType(routingContext.request().getHeader("Accept"));
            if (requestContext.isActive()) {
                doHandle(response, acceptHeader);
            } else {
                requestContext.activate();
                try {
                    doHandle(response, acceptHeader);
                } finally {
                    requestContext.terminate();
                }
            }
        }
    }

    private String chooseContentType(String acceptHeader) {
        if (acceptHeader == null) {
            return CONTENT_TYPE_OPENMETRICS_100;
        }
        if (acceptHeader.contains("text/plain") || acceptHeader.contains("text/html")) {
            return CONTENT_TYPE_004;
        }
        return CONTENT_TYPE_OPENMETRICS_100;
    }

    private void doHandle(HttpServerResponse response, String acceptHeader) {
        response.putHeader("Content-Type", acceptHeader)
                .end(Buffer.buffer(registry.scrape(acceptHeader)));
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
