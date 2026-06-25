package io.quarkus.resteasy.reactive.server.test.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class ServerLoggingFilterMultipartLargePayloadTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestResource.class))
            .overrideRuntimeConfigKey("quarkus.rest.logging.scope", "request-response")
            .overrideRuntimeConfigKey("quarkus.rest.logging.include-body", "true")
            .overrideRuntimeConfigKey("quarkus.rest.logging.body-buffer-limit", "1k")
            .setLogRecordPredicate(record -> "io.quarkus.rest.logging".equals(record.getLoggerName()))
            .assertLogRecords(records -> {
                List<String> messages = records.stream().map(LogRecord::getMessage).collect(Collectors.toList());

                // Body larger than bodyBufferLimit — noted without buffering; endpoint still succeeds
                String largeLog = messages.stream()
                        .filter(m -> m.startsWith("Request: POST") && m.contains("/log-test/large"))
                        .findFirst()
                        .orElseThrow();
                assertThat(largeLog).contains("bytes, body too large to log");

                // Chunked transfer — noted without buffering; endpoint still succeeds
                String chunkedLog = messages.stream()
                        .filter(m -> m.startsWith("Request: POST") && m.contains("/log-test/chunked"))
                        .findFirst()
                        .orElseThrow();
                assertThat(chunkedLog).contains("[multipart: chunked]");
            });

    @Test
    void testLargeMultipartBodyNotBuffered() throws IOException {
        // RestAssured's multiPart() uses Apache HttpClient which sends multipart bodies with
        // Transfer-Encoding: chunked (no Content-Length). Building the body manually and sending
        // it as a byte array forces Content-Length to be set, so the filter can detect the oversize
        // from the header without touching the stream.
        String boundary = "TestBoundary";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"large.bin\"\r\n")
                .getBytes(StandardCharsets.UTF_8));
        out.write("Content-Type: application/octet-stream\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        out.write(new byte[2048]);
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
        out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        byte[] body = out.toByteArray();

        RestAssured.given()
                .contentType("multipart/form-data; boundary=" + boundary)
                .body(body)
                .post("/log-test/large")
                .then()
                .statusCode(200);
    }

    @Test
    void testChunkedMultipartNotBuffered() throws Exception {
        // BodyPublishers.ofInputStream with unknown length causes HTTP/1.1 to send
        // Transfer-Encoding: chunked without Content-Length
        String boundary = "TestBoundary";
        String body = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"field\"\r\n\r\n"
                + "value\r\n"
                + "--" + boundary + "--\r\n";
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + RestAssured.port + "/log-test/chunked"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(bodyBytes)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Path("/log-test")
    public static class TestResource {

        @POST
        @Path("/large")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN)
        public String large(@RestForm FileUpload file) {
            return "ok";
        }

        @POST
        @Path("/chunked")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN)
        public String chunked(@RestForm String field) {
            return field != null ? field : "ok";
        }
    }
}
