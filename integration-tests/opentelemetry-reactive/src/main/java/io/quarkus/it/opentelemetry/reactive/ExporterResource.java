package io.quarkus.it.opentelemetry.reactive;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestQuery;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.ExceptionEventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.opentelemetry.runtime.tracing.InternalAttributes;

@Path("")
public class ExporterResource {
    @Inject
    CustomInMemorySpanExporter inMemorySpanExporter;

    @GET
    @Path("/reset")
    public Response reset() {
        inMemorySpanExporter.reset();
        return Response.ok().build();
    }

    @GET
    @Path("/export")
    public List<SpanData> export() {
        return inMemorySpanExporter.getFinishedSpanItems()
                .stream()
                .filter(sd -> !sd.getName().contains("export") && !sd.getName().contains("reset"))
                .collect(Collectors.toList());
    }

    @GET
    @Path("/export-event-attributes")
    public Map<String, Object> exportEventAttributes(@RestQuery String spanName, @RestQuery String eventName) {
        return export()
                .stream()
                .filter(s -> spanName.equals(s.getName()))
                .map(SpanData::getEvents)
                .flatMap(Collection::stream)
                .filter(e -> eventName.equals(e.getName()))
                .flatMap(e -> e.getAttributes().asMap().entrySet().stream())
                .collect(Collectors.toMap(e -> e.getKey().getKey(), Map.Entry::getValue));
    }

    @GET
    @Path("/exportExceptionMessages")
    public List<String> exportExceptionMessages() {
        return inMemorySpanExporter.getFinishedSpanItems()
                .stream()
                .filter(sd -> !sd.getName().contains("export") && !sd.getName().contains("reset"))
                .filter(sd -> !sd.getEvents().isEmpty())
                .flatMap(sd -> sd.getEvents().stream())
                .filter(e -> e instanceof ExceptionEventData)
                .map(e -> (ExceptionEventData) e)
                .map(e -> e.getException().getMessage())
                .collect(Collectors.toList());
    }

    @ApplicationScoped
    static class InMemorySpanExporterProducer {
        @Produces
        @Singleton
        CustomInMemorySpanExporter inMemorySpanExporter() {
            return new CustomInMemorySpanExporter();
        }
    }

    static class CustomInMemorySpanExporter implements SpanExporter {

        private final InMemorySpanExporter delegate = InMemorySpanExporter.create();

        public List<SpanData> getFinishedSpanItems() {
            return delegate.getFinishedSpanItems();
        }

        public void reset() {
            delegate.reset();
        }

        @Override
        public CompletableResultCode export(Collection<SpanData> spans) {
            return delegate.export(spans.stream()
                    .filter(sd -> !sd.getName().contains("export") && !sd.getName().contains("reset"))
                    .filter(sd -> !InternalAttributes.containsTraceless(sd.getAttributes()))
                    .collect(Collectors.toList()));
        }

        @Override
        public CompletableResultCode flush() {
            return delegate.flush();
        }

        @Override
        public CompletableResultCode shutdown() {
            return delegate.shutdown();
        }
    }
}
