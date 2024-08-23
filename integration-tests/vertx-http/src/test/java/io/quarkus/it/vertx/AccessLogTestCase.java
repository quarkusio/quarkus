package io.quarkus.it.vertx;

import static org.hamcrest.Matchers.containsString;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class AccessLogTestCase {

    /**
     * Fires a HTTP request, to an application which has access log enabled and then checks
     * the access-log contents to verify that the request was logged
     */
    @Test
    public void testAccessLogContent() {
        final Path logDirectory = Paths.get(".", "target");
        final Path accessLogFilePath = logDirectory.resolve("quarkus-access-log.log");
        final String queryParamVal = UUID.randomUUID().toString();
        final String targetUri = "/simple/access-log-test-endpoint?foo=" + queryParamVal;
        RestAssured.when().get(targetUri).then().body(containsString("passed"));
        Awaitility.given().pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(new ThrowingRunnable() {
                    @Override
                    public void run() throws Throwable {
                        Assertions.assertTrue(Files.exists(accessLogFilePath),
                                "access log file " + accessLogFilePath + " is missing");
                        String line = Files.readString(accessLogFilePath);
                        Assertions.assertTrue(line.startsWith("127.0.0.1 - - ["),
                                "access log doesn't contain request IP or does not wrap the date with []: " + line);
                        Assertions.assertTrue(line.contains("] \"GET"),
                                "access log doesn't contain the HTTP method or does not wrap the date with []: " + line);
                        Assertions.assertTrue(line.contains(targetUri),
                                "access log doesn't contain an entry for " + targetUri);
                        Assertions.assertTrue(line.contains("?foo=" + queryParamVal),
                                "access log is missing query params");
                        Assertions.assertFalse(line.contains("?foo=" + queryParamVal + "?foo=" + queryParamVal),
                                "access log contains duplicated query params");
                    }
                });
    }

}
