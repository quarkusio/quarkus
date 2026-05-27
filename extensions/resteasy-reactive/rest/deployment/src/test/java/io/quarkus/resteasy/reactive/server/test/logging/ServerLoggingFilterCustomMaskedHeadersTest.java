package io.quarkus.resteasy.reactive.server.test.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class ServerLoggingFilterCustomMaskedHeadersTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestResource.class))
            .overrideRuntimeConfigKey("quarkus.rest.logging.scope", "request-response")
            .overrideRuntimeConfigKey("quarkus.rest.logging.masked-headers", "X-Secret")
            .setLogRecordPredicate(record -> "io.quarkus.rest.logging".equals(record.getLoggerName()))
            .assertLogRecords(records -> {
                List<String> messages = records.stream().map(LogRecord::getMessage).collect(Collectors.toList());

                String requestLog = messages.stream()
                        .filter(m -> m.startsWith("Request: GET") && m.contains("/log-test/simple"))
                        .findFirst()
                        .orElseThrow();

                // custom header is masked
                assertThat(requestLog).contains("X-Secret=<hidden>");

                // Authorization is NOT masked — it was replaced by the custom list
                assertThat(requestLog).contains("Authorization=").doesNotContain("Authorization=<hidden>");
            });

    @Test
    void testCustomMaskedHeader() {
        RestAssured.given()
                .header("X-Secret", "super-secret-value")
                .header("Authorization", "Bearer visible-token")
                .get("/log-test/simple")
                .then()
                .statusCode(200);
    }

    @Path("/log-test")
    public static class TestResource {

        @GET
        @Path("/simple")
        @Produces(MediaType.TEXT_PLAIN)
        public String simple() {
            return "hello";
        }
    }
}
