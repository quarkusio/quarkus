package io.quarkus.micrometer.runtime.export.handlers;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.quarkus.micrometer.runtime.registry.json.JsonMeterRegistry;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class JsonHandler implements Handler<RoutingContext> {
    private static final Logger log = Logger.getLogger(JsonHandler.class);

    private JsonMeterRegistry registry;

    private boolean setup = false;

    @Override
    public void handle(RoutingContext routingContext) {
        if (!setup) {
            setup();
        }

        HttpServerResponse response = routingContext.response();
        if (registry == null) {
            response.setStatusCode(500)
                    .setStatusMessage("Unable to resolve JSON registry instance");
        } else {
            response.putHeader("Content-Type", "application/json")
                    .end(Buffer.buffer(registry.scrape()));
        }
    }

    private void setup() {
        Instance<JsonMeterRegistry> registries = CDI.current().select(JsonMeterRegistry.class,
                Default.Literal.INSTANCE);

        if (registries.isUnsatisfied()) {
            registry = null;
        } else if (registries.isAmbiguous()) {
            registry = registries.iterator().next();
            log.warnf("Multiple JSON registries present. Using %s with the built in scrape endpoint", registry);
        } else {
            registry = registries.get();
        }

        setup = true;
    }
}
