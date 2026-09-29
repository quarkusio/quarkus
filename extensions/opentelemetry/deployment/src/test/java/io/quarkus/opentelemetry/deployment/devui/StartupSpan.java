package io.quarkus.opentelemetry.deployment.devui;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.opentelemetry.api.trace.Tracer;
import io.quarkus.runtime.StartupEvent;

/**
 * Ends a span while the application is still starting, the way a migration or a {@code @Startup} bean would. The
 * lowest priority makes it one of the first startup observers to run.
 */
@ApplicationScoped
public class StartupSpan {

    @Inject
    Tracer tracer;

    void onStart(@Observes @Priority(1) StartupEvent startup) {
        tracer.spanBuilder("startup-work").startSpan().end();
    }
}
