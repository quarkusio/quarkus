package io.quarkus.it.opentelemetry;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.TestLog;
import io.quarkus.test.junit.QuarkusTest;

/**
 * The application sets both a service.name that differs from the application name and a
 * service.instance.id. Since OpenTelemetry SDK 1.67 a resource also carries entities, and writing an
 * attribute that identifies one of them removes the entity association and says so in the log. The
 * deployment tests assert the entities directly; this one checks the thing a user would actually
 * notice, in a launched application, in JVM and in native mode.
 */
@QuarkusTest
public class ResourceWarningTest {

    private static final String RESOURCE_LOGGER = "io.opentelemetry.sdk.resources";
    private static final String ENVIRONMENT_RESOURCE_LOGGER = "io.opentelemetry.sdk.autoconfigure.EnvironmentResource";

    protected TestLog testLog;

    @Test
    public void startupLogHasNoResourceWarning() throws IOException {
        Path logFile = testLog.getLogFilePath();
        assertThat(logFile).exists();

        List<String> offending = Files.readAllLines(logFile).stream()
                .filter(line -> line.contains("WARN") || line.contains("ERROR"))
                .filter(line -> line.contains(RESOURCE_LOGGER) || line.contains(ENVIRONMENT_RESOURCE_LOGGER))
                .toList();

        assertThat(offending)
                .as("the OpenTelemetry SDK must build the resource without complaining")
                .isEmpty();
    }
}
