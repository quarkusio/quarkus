package io.quarkus.it.jaxb;

import static io.quarkus.runtime.graal.AwtImageIO.AWT_EXTENSION_HINT;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.RestAssured;

@QuarkusIntegrationTest
public class JaxbAWTIT {
    public static Pattern AWT_EXTENSION_HINT_PATTERN = Pattern.compile(".*" + AWT_EXTENSION_HINT + ".*");

    public static final String BOOK_WITH_IMAGE = "<book>" +
            "<cover>iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAIElEQVR4XmNgGCngPxSgi6MAZAU4FeOUQAdEKwQBdKsBOgof4SXid6kAAAAASUVORK5CYII=</cover>"
            +
            "<title>Foundation</title>" +
            "</book>";
    public static final String BOOK_WITHOUT_IMAGE = "<book>" +
            "<title>Foundation</title>" +
            "</book>";

    public static final String CONTENT_TYPE = "application/xml; charset=UTF-8";

    @Test
    public void bookNoCover() {
        RestAssured.given()
                .when()
                .header("Content-Type", CONTENT_TYPE)
                .body(BOOK_WITHOUT_IMAGE)
                .when()
                .post("/jaxb/book")
                .then()
                .statusCode(HttpStatus.SC_ACCEPTED)
                .body(is("No Cover"));
    }

    @Test
    public void book() {
        RestAssured.given()
                .when()
                .header("Content-Type", CONTENT_TYPE)
                .body(BOOK_WITH_IMAGE)
                .when()
                .post("/jaxb/book")
                .then()
                .statusCode(HttpStatus.SC_ACCEPTED)
                .body(is("No Cover"));
        checkLog(AWT_EXTENSION_HINT_PATTERN);
    }

    /**
     * Looks for a pattern in the log, line by line.
     *
     * @param lineMatchRegexp pattern
     */
    private static void checkLog(final Pattern lineMatchRegexp) {
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
