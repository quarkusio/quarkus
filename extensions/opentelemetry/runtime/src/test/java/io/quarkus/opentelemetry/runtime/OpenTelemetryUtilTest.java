package io.quarkus.opentelemetry.runtime;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

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
    public void testNormalizeDuration_correct_duration_value() {
        List<String> collect = Stream.of("10S", "5s", "PT20.345S")
                .map(OpenTelemetryUtil::normalizeDuration).collect(Collectors.toList());
        Assertions.assertThat(collect).containsExactly(
                "10000ms",
                "5000ms",
                "20345ms");
    }

    @Test
    public void testNormalizeDuration_duration_wrong_value() {
        Assertions.assertThatThrownBy(() -> OpenTelemetryUtil.normalizeDuration("10PTS3"));
    }

    @Test
    public void testNormalizeProperty_property_as_duration() {
        String normalizeProperty = OpenTelemetryUtil.normalizeProperty("quarkus.otel.bsp.schedule.delay", "100S");
        Assertions.assertThat(normalizeProperty).isEqualTo("100000ms");
    }

    @Test
    void testNormalizeProperty_property_without_time_unit() {
        String normalizeProperty = OpenTelemetryUtil.normalizeProperty("quarkus.otel.bsp.schedule.delay", "50");
        Assertions.assertThat(normalizeProperty).isEqualTo("50000ms");
    }

    @Test
    public void testNormalizeProperty_property_without_converter() {
        String normalizedProperty = OpenTelemetryUtil.normalizeProperty("quarkus.otel.exporter.otlp.traces.endpoint",
                "http://localhost:4317/");
        Assertions.assertThat(normalizedProperty).isEqualTo("http://localhost:4317/");
    }
}
