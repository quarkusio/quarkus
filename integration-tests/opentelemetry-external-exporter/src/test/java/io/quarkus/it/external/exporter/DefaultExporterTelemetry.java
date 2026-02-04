package io.quarkus.it.external.exporter;

import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;

public class DefaultExporterTelemetry {

    private final Traces traces;

    public DefaultExporterTelemetry(Traces traces) {
        this.traces = traces;
    }

    public void verifyExportedTraces() {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(traces.getTraceRequests()).hasSize(1));

        ExportTraceServiceRequest request = traces.getTraceRequests().get(0);
        assertThat(request.getResourceSpansCount()).isEqualTo(1);

        ResourceSpans resourceSpans = request.getResourceSpans(0);
        assertThat(resourceSpans.getResource().getAttributesList())
                .contains(KeyValue.newBuilder()
                        .setKey(SERVICE_NAME.getKey())
                        .setValue(AnyValue.newBuilder()
                                .setStringValue("opentelemetry-external-exporter-integration-test").build())
                        .build());
        assertThat(resourceSpans.getScopeSpansCount()).isEqualTo(1);

        ScopeSpans scopeSpans = resourceSpans.getScopeSpans(0);
        assertThat(scopeSpans.getSpansCount()).isEqualTo(1);

        Span span = scopeSpans.getSpans(0);
        assertThat(span.getName()).isEqualTo("GET /hello");
        assertThat(span.getAttributesList())
                .contains(KeyValue.newBuilder()
                        .setKey("http.request.method")
                        .setValue(AnyValue.newBuilder()
                                .setStringValue("GET").build())
                        .build());
    }

    public void verifyNoExportedTraces() {
        assertThat(traces.getTraceRequests()).isEmpty();
    }
}
