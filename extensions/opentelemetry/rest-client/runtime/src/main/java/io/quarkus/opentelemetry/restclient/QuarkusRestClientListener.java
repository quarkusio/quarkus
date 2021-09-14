package io.quarkus.opentelemetry.restclient;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.spi.RestClientListener;
import org.jboss.logging.Logger;

import io.opentelemetry.api.trace.Tracer;

public class QuarkusRestClientListener implements RestClientListener {
    private static final Logger log = Logger.getLogger(QuarkusRestClientListener.class);

    Tracer tracer;
    boolean initialized = false;
    boolean tracingEnabled = false;

    @Override
    public void onNewClient(Class<?> clientInterface, RestClientBuilder restClientBuilder) {
        if (setupTracer()) {
            restClientBuilder.register(new ClientTracingFilter(tracer));
        }
    }

    private boolean setupTracer() {
        if (!initialized) {
            Instance<Tracer> tracerInstance = CDI.current().select(Tracer.class);

            if (tracerInstance.isResolvable()) {
                tracer = tracerInstance.get();
                tracingEnabled = true;
            } else {
                log.warn("Unable to resolve Tracer instance, no ClientTracingFilter registered on REST Client.");
            }
            initialized = true;
        }
        return tracingEnabled;
    }
}
