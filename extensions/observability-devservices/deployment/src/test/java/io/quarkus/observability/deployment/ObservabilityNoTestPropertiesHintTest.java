package io.quarkus.observability.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Optional;
import java.util.logging.LogRecord;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that the hint stays silent when the property is user-defined or optional.
 */
public class ObservabilityNoTestPropertiesHintTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(GrafanaClient.class))
            .overrideConfigKey("grafana.endpoint", "localhost:3000")
            .setLogRecordPredicate(record -> record.getMessage().contains("quarkus.observability.enabled-in-tests")
                    || String.join(",", parameters(record)).contains("quarkus.observability.enabled-in-tests"))
            .assertLogRecords(records -> assertThat(records).isEmpty());

    @Test
    void applicationStarts() {
    }

    private static String[] parameters(LogRecord record) {
        if (record.getParameters() == null) {
            return new String[0];
        }
        return Arrays.stream(record.getParameters()).map(String::valueOf).toArray(String[]::new);
    }

    @Unremovable
    @ApplicationScoped
    public static class GrafanaClient {

        @ConfigProperty(name = "grafana.endpoint")
        String endpoint;

        @ConfigProperty(name = "otel-collector.url")
        Optional<String> otelCollectorUrl;
    }
}
