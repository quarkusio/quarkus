package io.quarkus.it.opentelemetry.minimal;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

@ApplicationScoped
public class HelloService {

    @Inject
    Vertx vertx;

    @Inject
    InMemoryMetricExporter inMemoryMetricExporter;

    void onStart(@Observes StartupEvent ev) {
        // Code executed during application startup
        System.out.println("Application is starting...");
    }

    public Uni<Integer> getMetricCount() {
        return Uni.createFrom().emitter(e -> {
            vertx.setTimer(100, x -> e.complete(inMemoryMetricExporter.getFinishedMetricItems().size()));
        });
    }

    @ApplicationScoped
    static class InMemoryMetricExporterProducer {
        @Produces
        @Singleton
        InMemoryMetricExporter inMemoryMetricsExporter() {
            return InMemoryMetricExporter.create();
        }
    }
}
