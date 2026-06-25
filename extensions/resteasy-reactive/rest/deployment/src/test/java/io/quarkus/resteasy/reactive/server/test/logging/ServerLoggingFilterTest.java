package io.quarkus.resteasy.reactive.server.test.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class ServerLoggingFilterTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestResource.class))
            .overrideRuntimeConfigKey("quarkus.rest.logging.scope", "request-response")
            .overrideRuntimeConfigKey("quarkus.rest.logging.include-body", "true")
            .overrideRuntimeConfigKey("quarkus.rest.logging.body-limit", "10")
            .setLogRecordPredicate(record -> "io.quarkus.rest.logging".equals(record.getLoggerName()))
            .assertLogRecords(records -> {
                List<String> messages = records.stream().map(LogRecord::getMessage).collect(Collectors.toList());

                // GET request is logged with method, URI, headers and empty body indicator
                String getRequestLog = messages.stream()
                        .filter(m -> m.startsWith("Request: GET") && m.contains("/log-test/simple"))
                        .findFirst()
                        .orElseThrow();
                assertThat(getRequestLog)
                        .contains("Headers[")
                        .contains("Empty body");

                // POST request body is logged (5 chars, not truncated at limit 10)
                String postRequestLog = messages.stream()
                        .filter(m -> m.startsWith("Request: POST") && m.contains("Body:\nhello"))
                        .findFirst()
                        .orElseThrow();
                assertThat(postRequestLog)
                        .contains("/log-test/echo");

                // POST response body is logged — echo endpoint returns the same body
                String postResponseLog = messages.stream()
                        .filter(m -> m.startsWith("Response: POST") && m.contains("/log-test/echo")
                                && !m.contains("[truncated]"))
                        .findFirst()
                        .orElseThrow();
                assertThat(postResponseLog)
                        .contains("Status[200 OK]")
                        .contains("Body:\nhello");

                // GET response body is logged and truncated ("hello world" exceeds limit of 10)
                String getResponseLog = messages.stream()
                        .filter(m -> m.startsWith("Response: GET") && m.contains("/log-test/simple"))
                        .findFirst()
                        .orElseThrow();
                assertThat(getResponseLog)
                        .contains("Status[200 OK]")
                        .contains("Headers[")
                        .contains("Body:\nhello worl...[truncated]");

                // Authorization and Cookie header values are both masked in a single request log
                String maskedHeadersLog = messages.stream()
                        .filter(m -> m.startsWith("Request:") && m.contains("Authorization=<hidden>"))
                        .findFirst()
                        .orElseThrow();
                assertThat(maskedHeadersLog)
                        .contains("Cookie=<hidden>");

                // POST request body exceeding the limit is truncated (first 10 chars of the sent body)
                String truncatedRequestLog = messages.stream()
                        .filter(m -> m.startsWith("Request: POST") && m.contains("...[truncated]"))
                        .findFirst()
                        .orElseThrow();
                assertThat(truncatedRequestLog)
                        .contains("Body:\nthis is a ...[truncated]");

                // Bodyless response (204) is logged without a Body line
                String bodylessResponseLog = messages.stream()
                        .filter(m -> m.startsWith("Response: DELETE") && m.contains("/log-test/delete"))
                        .findFirst()
                        .orElseThrow();
                assertThat(bodylessResponseLog)
                        .contains("Status[204 No Content]")
                        .doesNotContain("Body:");

                // Large request body is truncated at body-limit; endpoint still receives and echoes full body
                String largeRequestLog = messages.stream()
                        .filter(m -> m.startsWith("Request: POST") && m.contains("/log-test/large"))
                        .findFirst()
                        .orElseThrow();
                assertThat(largeRequestLog)
                        .contains("Body:\nAAAAAAAAAA...[truncated]");
                String largeResponseLog = messages.stream()
                        .filter(m -> m.startsWith("Response: POST") && m.contains("/log-test/large"))
                        .findFirst()
                        .orElseThrow();
                assertThat(largeResponseLog)
                        .contains("Status[200 OK]")
                        .contains("...[truncated]");

                // Binary request body is logged as byte count, not as string content
                String binaryRequestLog = messages.stream()
                        .filter(m -> m.startsWith("Request: POST") && m.contains("/log-test/binary"))
                        .findFirst()
                        .orElseThrow();
                assertThat(binaryRequestLog)
                        .contains("Body: [5 bytes]")
                        .doesNotContain("Body:\n");

                // Binary response body is logged as byte count, not as string content
                String binaryResponseLog = messages.stream()
                        .filter(m -> m.startsWith("Response: POST") && m.contains("/log-test/binary"))
                        .findFirst()
                        .orElseThrow();
                assertThat(binaryResponseLog)
                        .contains("Body: [binary 5 bytes]")
                        .doesNotContain("Body:\n");
            });

    @Test
    void testGetRequestAndResponseLogged() {
        RestAssured.get("/log-test/simple")
                .then()
                .statusCode(200);
    }

    @Test
    void testPostRequestBodyLogged() {
        RestAssured.given()
                .contentType(MediaType.TEXT_PLAIN)
                .body("hello")
                .post("/log-test/echo")
                .then()
                .statusCode(200);
    }

    @Test
    void testSensitiveHeadersMasked() {
        RestAssured.given()
                .header("Authorization", "Bearer secret-token")
                .header("Cookie", "session=abc123")
                .get("/log-test/simple")
                .then()
                .statusCode(200);
    }

    @Test
    void testBodyTruncated() {
        RestAssured.given()
                .contentType(MediaType.TEXT_PLAIN)
                .body("this is a long body exceeding the limit")
                .post("/log-test/echo")
                .then()
                .statusCode(200);
    }

    @Test
    void testBodylessResponse() {
        RestAssured.delete("/log-test/delete")
                .then()
                .statusCode(204);
    }

    @Test
    void testLargeBodyTruncated() {
        RestAssured.given()
                .contentType(MediaType.TEXT_PLAIN)
                .body("A".repeat(100_000))
                .post("/log-test/large")
                .then()
                .statusCode(200);
    }

    @Test
    void testBinaryBody() {
        RestAssured.given()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new byte[] { 1, 2, 3, 4, 5 })
                .post("/log-test/binary")
                .then()
                .statusCode(200);
    }

    @Path("/log-test")
    public static class TestResource {

        @GET
        @Path("/simple")
        @Produces(MediaType.TEXT_PLAIN)
        public String simple() {
            return "hello world";
        }

        @POST
        @Path("/echo")
        @Consumes(MediaType.TEXT_PLAIN)
        @Produces(MediaType.TEXT_PLAIN)
        public String echo(String body) {
            return body;
        }

        @jakarta.ws.rs.DELETE
        @Path("/delete")
        public Response delete() {
            return Response.noContent().build();
        }

        @GET
        @Path("/binary")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public byte[] binaryGet() {
            return new byte[] { 1, 2, 3, 4, 5 };
        }

        @POST
        @Path("/binary")
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public byte[] binaryPost(byte[] body) {
            return body;
        }

        @POST
        @Path("/large")
        @Consumes(MediaType.TEXT_PLAIN)
        @Produces(MediaType.TEXT_PLAIN)
        public String large(String body) {
            return body;
        }
    }
}
