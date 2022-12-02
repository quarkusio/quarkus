package io.quarkus.it.vertx;

import static org.hamcrest.Matchers.containsString;

import java.nio.charset.StandardCharsets;
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
     *
     * @throws Exception
     */
    @Test
    public void testAccessLogContent() throws Exception {
        final Path logDirectory = Paths.get(".", "target");
        final String queryParamVal = UUID.randomUUID().toString();
        final String targetUri = "/simple/access-log-test-endpoint?foo=" + queryParamVal;
        RestAssured.when().get(targetUri).then().body(containsString("passed"));
        Awaitility.given().pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(new ThrowingRunnable() {
                    @Override
                    public void run() throws Throwable {
                        final Path accessLogFilePath = logDirectory.resolve("quarkus-access-log.log");
                        Assertions.assertTrue(Files.exists(accessLogFilePath),
                                "access log file " + accessLogFilePath + " is missing");
                        String data = new String(Files.readAllBytes(accessLogFilePath), StandardCharsets.UTF_8);
                        Assertions.assertTrue(data.contains(targetUri),
                                "access log doesn't contain an entry for " + targetUri);
                        Assertions.assertTrue(data.contains("?foo=" + queryParamVal),
                                "access log is missing query params");
                        Assertions.assertFalse(data.contains("?foo=" + queryParamVal + "?foo=" + queryParamVal),
                                "access log contains duplicated query params");
                    }
                });
    }

}
