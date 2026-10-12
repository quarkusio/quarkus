package io.quarkus.observability.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that a hint is logged when the application requires a property provided by observability Dev Services,
 * which are not started in test mode by default.
 */
public class ObservabilityMissingTestPropertiesHintTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(GrafanaClient.class))
            .assertException(t -> assertThat(t).hasStackTraceContaining("grafana.endpoint"))
            .setLogRecordPredicate(record -> record.getMessage().contains("quarkus.observability.enabled-in-tests"))
            .assertLogRecords(records -> assertThat(records)
                    .anySatisfy(record -> {
                        assertThat(record.getLevel()).isEqualTo(Level.WARNING);
                        assertThat(record.getMessage() + String.join(",", parameters(record))).contains("grafana.endpoint");
                    }));

    @Test
    void applicationDoesNotStart() {
        fail("The application should not have started");
    }

    private static String[] parameters(LogRecord record) {
        if (record.getParameters() == null) {
            return new String[0];
        }
        return java.util.Arrays.stream(record.getParameters()).map(String::valueOf).toArray(String[]::new);
    }

    @Unremovable
    @ApplicationScoped
    public static class GrafanaClient {

        @ConfigProperty(name = "grafana.endpoint")
        String endpoint;
    }
}
