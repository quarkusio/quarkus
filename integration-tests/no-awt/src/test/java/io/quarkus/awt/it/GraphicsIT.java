package io.quarkus.awt.it;

import static io.quarkus.runtime.graal.AwtImageIO.AWT_EXTENSION_HINT;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.http.HttpStatus;
import org.jboss.logging.Logger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.junit.NativeImageTest;
import io.restassured.RestAssured;

@NativeImageTest
public class GraphicsIT {

    private static final Logger LOG = Logger.getLogger(GraphicsIT.class);

    public static Pattern AWT_EXTENSION_HINT_PATTERN = Pattern.compile(".*" + AWT_EXTENSION_HINT + ".*");

    @ParameterizedTest
    @ValueSource(strings = {
            "IIORegistry",
            "GraphicsEnvironment",
            "Color",
            "BufferedImage",
            "Transformations",
            "ConvolveOp",
            "Font",
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
    static void checkLog(final Pattern lineMatchRegexp) {
        final Path logFilePath = Paths.get(".", "target", "quarkus.log").toAbsolutePath();
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

}
