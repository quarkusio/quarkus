package io.quarkus.it.opentelemetry.vertx;

import static java.util.Comparator.comparingLong;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;

@ApplicationScoped
public class ExporterRouter {
    @Inject
    Router router;
    @Inject
    InMemorySpanExporter exporter;

    public void register(@Observes StartupEvent ev) {
        router.get("/reset").handler(rc -> {
            exporter.reset();
            rc.response().end();
        });

        router.get("/export").handler(rc -> {
            List<SpanData> export = exporter.getFinishedSpanItems()
                    .stream()
                    .filter(sd -> !sd.getName().contains("export") && !sd.getName().contains("reset")
                            && !sd.getName().contains("bus/messages"))
                    .sorted(comparingLong(SpanData::getStartEpochNanos).reversed())
                    .collect(Collectors.toList());

            rc.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(export));
        });
    }

    @ApplicationScoped
    static class InMemorySpanExporterProducer {
        @Produces
        @Singleton
        InMemorySpanExporter inMemorySpanExporter() {
            return InMemorySpanExporter.create();
        }
    }
}
