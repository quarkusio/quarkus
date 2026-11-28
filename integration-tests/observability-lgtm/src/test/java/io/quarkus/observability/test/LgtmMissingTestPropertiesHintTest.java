package io.quarkus.observability.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that a hint is logged when the application requires a property provided by observability Dev Services,
 * which are not started in test mode by default.
 */
@Tag("extension")
public class LgtmMissingTestPropertiesHintTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(GrafanaClient.class)
                    .addAsResource(new StringAsset("hint-test=missing\n"), "application.properties"))
            .overrideConfigKey("quarkus.observability.enabled-in-tests", "false")
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
        return Arrays.stream(record.getParameters()).map(String::valueOf).toArray(String[]::new);
    }

    @Unremovable
    @ApplicationScoped
    @IfBuildProperty(name = "hint-test", stringValue = "missing")
    public static class GrafanaClient {

        @ConfigProperty(name = "grafana.endpoint")
        String endpoint;
    }
}
