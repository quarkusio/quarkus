package io.quarkus.awt.it;

import static io.quarkus.runtime.graal.AwtImageIO.AWT_EXTENSION_HINT;
import static io.quarkus.test.junit.QuarkusIntegrationTestExtension.TEST_LOG_PATH;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.http.HttpStatus;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.runtime.logging.LogRuntimeConfig;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.value.registry.ValueRegistry;
import io.restassured.RestAssured;
import io.smallrye.config.SmallRyeConfig;

@QuarkusIntegrationTest
public class GraphicsIT {

    private static final Logger LOG = Logger.getLogger(GraphicsIT.class);

    public static Pattern AWT_EXTENSION_HINT_PATTERN = Pattern.compile(".*" + AWT_EXTENSION_HINT + ".*");

    // Injected automatically by QuarkusIntegrationTestExtension
    private ValueRegistry valueRegistry;

    @ParameterizedTest
    @ValueSource(strings = {
            "IIORegistry",
            "GraphicsEnvironment",
            "Color",
            "BufferedImage",
            "Transformations",
            "ConvolveOp",
            "Path2D",
            "ImageReader",
            "ImageWriter"
    })
    public void testGraphics(String entrypoint) throws IOException {
        LOG.infof("Triggering test: %s", entrypoint);
        RestAssured.given().when()
                .param("entrypoint", entrypoint)
                .get("/graphics")
                .then()
                .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                .log().all();
        checkLog(AWT_EXTENSION_HINT_PATTERN);
    }

    /**
     * Looks for a pattern in the log, line by line.
     *
     * @param lineMatchRegexp pattern
     */
    private void checkLog(final Pattern lineMatchRegexp) {
        final Path logFilePath = getLogPath();
        org.awaitility.Awaitility.given().pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(3, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertTrue(Files.exists(logFilePath), "Quarkus log file " + logFilePath + " is missing");
                    boolean found = false;
                    final StringBuilder sbLog = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(logFilePath)),
                                    StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sbLog.append(line).append("\r\n");
                            found = lineMatchRegexp.matcher(line).matches();
                            if (found) {
                                break;
                            }
                        }
                    }
                    assertTrue(found, "Pattern " + lineMatchRegexp.pattern() + " not found in log " + logFilePath + ". \n" +
                            "The log was: " + sbLog);
                });
    }

    private Path getLogPath() {
        if (valueRegistry != null && valueRegistry.containsKey(TEST_LOG_PATH)) {
            return valueRegistry.get(TEST_LOG_PATH);
        }
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        LogRuntimeConfig logRuntimeConfig = config.getConfigMapping(LogRuntimeConfig.class);
        return logRuntimeConfig.file().path().toPath();
    }
}
