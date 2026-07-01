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
import io.smallrye.mutiny.Multi;

public class ServerLoggingFilterSseTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(SseResource.class))
            .overrideRuntimeConfigKey("quarkus.rest.logging.scope", "request-response")
            .overrideRuntimeConfigKey("quarkus.rest.logging.include-body", "true")
            .setLogRecordPredicate(record -> "io.quarkus.rest.logging".equals(record.getLoggerName()))
            .assertLogRecords(records -> {
                List<String> messages = records.stream().map(LogRecord::getMessage).collect(Collectors.toList());

                // SSE request is logged normally
                String requestLog = messages.stream()
                        .filter(m -> m.startsWith("Request: GET") && m.contains("/log-test/sse"))
                        .findFirst()
                        .orElseThrow();
                assertThat(requestLog)
                        .contains("Headers[")
                        .contains("Empty body");

                // SSE response is logged via the headers-end handler with streaming indicator
                String responseLog = messages.stream()
                        .filter(m -> m.startsWith("Response: GET") && m.contains("/log-test/sse"))
                        .findFirst()
                        .orElseThrow();
                assertThat(responseLog)
                        .contains("Status[200 OK]")
                        .contains("Headers[")
                        .contains("Body: [streaming]");
            });

    @Test
    void testSseResponseLogged() {
        RestAssured.given()
                .accept("text/event-stream")
                .get("/log-test/sse")
                .then()
                .statusCode(200);
    }

    @Path("/log-test")
    public static class SseResource {

        @GET
        @Path("/sse")
        @Produces(MediaType.SERVER_SENT_EVENTS)
        public Multi<String> sse() {
            return Multi.createFrom().items("a", "b", "c");
        }
    }
}
