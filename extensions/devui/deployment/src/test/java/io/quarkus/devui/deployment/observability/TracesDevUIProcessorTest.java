package io.quarkus.devui.deployment.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.devui.spi.observability.TracesBackendBuildItem;

public class TracesDevUIProcessorTest {

    @Test
    public void theTracesCardIsNamedAfterTheTracer() {
        assertThat(TracesDevUIProcessor.signalTitle(List.of(new TracesBackendBuildItem("otel", "OpenTelemetry Traces"))))
                .isEqualTo("OpenTelemetry Traces");
    }

    @Test
    public void withSeveralTracersTheTitleDoesNotDependOnTheBuildOrder() {
        TracesBackendBuildItem otel = new TracesBackendBuildItem("otel", "OpenTelemetry Traces");
        TracesBackendBuildItem other = new TracesBackendBuildItem("other", "Other Traces");

        assertThat(TracesDevUIProcessor.signalTitle(List.of(otel, other)))
                .isEqualTo(TracesDevUIProcessor.signalTitle(List.of(other, otel)))
                .isEqualTo("Traces (otel, other)");
    }
}
