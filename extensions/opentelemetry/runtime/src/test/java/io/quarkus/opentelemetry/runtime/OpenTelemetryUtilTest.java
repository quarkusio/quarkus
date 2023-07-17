package io.quarkus.opentelemetry.runtime;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

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
}
