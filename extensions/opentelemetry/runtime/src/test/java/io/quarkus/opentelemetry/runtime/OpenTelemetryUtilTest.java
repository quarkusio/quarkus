package io.quarkus.opentelemetry.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.quarkus.opentelemetry.runtime.config.runtime.SemconvStabilityType;

public class OpenTelemetryUtilTest {

    @Test
    public void testConvertKeyValueListToMap() {
        Map<String, String> actual = OpenTelemetryUtil
                .convertKeyValueListToMap(Arrays.asList(
                        "service.name=myservice",
                        "service.version=1.0",
                        "deployment.environment=production"));
        Assertions.assertThat(actual).containsExactly(
                new AbstractMap.SimpleEntry<>("service.name", "myservice"),
                new AbstractMap.SimpleEntry<>("service.version", "1.0"),
                new AbstractMap.SimpleEntry<>("deployment.environment", "production"));
    }

    @Test
    public void testConvertKeyValueListToMap_skip_empty_values() {
        Map<String, String> actual = OpenTelemetryUtil
                .convertKeyValueListToMap(Arrays.asList(
                        "service.name=myservice",
                        "service.version=1.0",
                        "deployment.environment=production",
                        ""));
        Assertions.assertThat(actual).containsExactly(
                new AbstractMap.SimpleEntry<>("service.name", "myservice"),
                new AbstractMap.SimpleEntry<>("service.version", "1.0"),
                new AbstractMap.SimpleEntry<>("deployment.environment", "production"));
    }

    @Test
    public void testConvertKeyValueListToMap_last_value_takes_precedence() {
        Map<String, String> actual = OpenTelemetryUtil
                .convertKeyValueListToMap(Arrays.asList(
                        "service.name=myservice to be overwritten",
                        "service.version=1.0",
                        "deployment.environment=production",
                        "service.name=myservice",
                        ""));
        Assertions.assertThat(actual).containsExactly(
                new AbstractMap.SimpleEntry<>("service.name", "myservice"),
                new AbstractMap.SimpleEntry<>("service.version", "1.0"),
                new AbstractMap.SimpleEntry<>("deployment.environment", "production"));
    }

    @Test
    public void testConvertKeyValueListToMap_empty_value() {
        Map<String, String> actual = OpenTelemetryUtil
                .convertKeyValueListToMap(Collections.emptyList());
        Assertions.assertThat(actual).containsExactly();
    }

    @Test
    public void testGetSpanData() {
        SpanProcessor mockedSpanProcessor = mock(SpanProcessor.class);

        SdkTracerProvider tracerSdkFactory = SdkTracerProvider.builder()
                .addSpanProcessor(mockedSpanProcessor)
                .build();
        Tracer spanBuilderSdkTest = tracerSdkFactory.get("SpanBuilderSdkTest");
        SpanBuilder spanBuilder = spanBuilderSdkTest.spanBuilder("SpanName");

        Span parent = spanBuilder.startSpan();
        Context contextParent = Context.current().with(parent);

        Span child = spanBuilder.setParent(contextParent).startSpan();
        Context contextChild = Context.current().with(child);

        Map<String, String> actual = OpenTelemetryUtil.getSpanData(contextChild);
        assertEquals(4, actual.size());
        assertEquals(child.getSpanContext().getSpanId(), actual.get("spanId"));
        assertEquals(child.getSpanContext().getTraceId(), actual.get("traceId"));
        assertEquals("true", actual.get("sampled"));
        assertEquals(parent.getSpanContext().getSpanId(), actual.get("parentId"));
    }

    @Test
    public void testGetSpanData_noParent() {
        SpanProcessor mockedSpanProcessor = mock(SpanProcessor.class);
        SdkTracerProvider tracerSdkFactory = SdkTracerProvider.builder()
                .addSpanProcessor(mockedSpanProcessor)
                .build();
        Tracer spanBuilderSdkTest = tracerSdkFactory.get("SpanBuilderSdkTest");

        SpanBuilder spanBuilder = spanBuilderSdkTest.spanBuilder("SpanName");

        Span child = spanBuilder.startSpan();
        Context contextChild = Context.current().with(child);

        Map<String, String> actual = OpenTelemetryUtil.getSpanData(contextChild);
        assertEquals(3, actual.size());
        assertEquals(child.getSpanContext().getSpanId(), actual.get("spanId"));
        assertEquals(child.getSpanContext().getTraceId(), actual.get("traceId"));
        assertEquals("true", actual.get("sampled"));
    }

    @Test
    public void testGetSpanData_nullValue() {
        Map<String, String> actual = OpenTelemetryUtil.getSpanData(null);
        assertEquals(0, actual.size());
    }

    @Test
    public void testSemconvOptin() {
        assertEquals(SemconvStabilityType.HTTP_OLD,
                OpenTelemetryUtil.getSemconvStabilityOptin(null));
        assertEquals(SemconvStabilityType.HTTP_OLD,
                OpenTelemetryUtil.getSemconvStabilityOptin(SemconvStabilityType.HTTP_OLD.getValue()));
        assertEquals(SemconvStabilityType.HTTP,
                OpenTelemetryUtil.getSemconvStabilityOptin(SemconvStabilityType.HTTP.getValue()));
        assertEquals(SemconvStabilityType.HTTP_DUP,
                OpenTelemetryUtil.getSemconvStabilityOptin(SemconvStabilityType.HTTP_DUP.getValue()));
    }
}
