package io.quarkus.it.opentelemetry.vertx;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.runtime.StartupEvent;
import io.vertx.ext.web.Router;

@ApplicationScoped
public class ExporterRouter {
    @Inject
    Router router;
    @Inject
    InMemorySpanExporter exporter;
    // The managed mapper is customized by SpanAttributesJsonMapperCustomizer so that a SpanData's
    // attributes are serialized as a flat {key: value} map (see that class for the why).
    @Inject
    ObjectMapper mapper;

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
                    .collect(Collectors.toList());

            String body;
            try {
                body = mapper.writeValueAsString(export);
            } catch (JsonProcessingException e) {
                rc.fail(e);
                return;
            }

            rc.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(body);
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
