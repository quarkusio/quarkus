package io.quarkus.resteasy.reactive.server.test.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/*
 Verifies that a large, custom body-limit doesn't let a multipart request get buffered in full —
 buffering is capped at min(max(body-limit, an internal floor), an internal ceiling), so even though
 body-limit here is configured well above that ceiling, the request is still noted in the log
 without buffering the body into memory. The payload (2 GiB) is deliberately bigger than the test
 JVM's heap (build-parent sets surefire's argLine to -Xmx1500m), so if this feature ever regressed
 into buffering the body fully in memory, the test would fail with an OutOfMemoryError instead of
 silently passing. The request is streamed from a sparse file (so no gigabytes are actually written
 to disk) and the response is drained by counting bytes rather than collecting them, so nothing on
 either side ever holds the whole body at once.
 */
public class ServerLoggingFilterMultipartLargePayloadTest {

    private static final long LARGE_FILE_SIZE = 2L * 1024 * 1024 * 1024; // 2 GiB — bigger than the test JVM's heap

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestResource.class))
            .overrideRuntimeConfigKey("quarkus.rest.logging.scope", "request-response")
            .overrideRuntimeConfigKey("quarkus.rest.logging.include-body", "true")
            // Larger than the internal safety ceiling — proves that ceiling still clamps buffering
            .overrideRuntimeConfigKey("quarkus.rest.logging.body-limit", "50000")
            .overrideRuntimeConfigKey("quarkus.http.limits.max-body-size", "3G") // default 10240K rejects our 2 GiB payload
            .setLogRecordPredicate(record -> "io.quarkus.rest.logging".equals(record.getLoggerName()))
            .assertLogRecords(records -> {
                List<String> messages = records.stream().map(LogRecord::getMessage).collect(Collectors.toList());

                // Request body larger than the safety ceiling despite the raised body-limit — noted without
                // buffering; endpoint still succeeds
                String largeLog = messages.stream()
                        .filter(m -> m.startsWith("Request: POST") && m.contains("/log-test/large"))
                        .findFirst()
                        .orElseThrow();
                assertThat(largeLog).contains("bytes, body too large to log");

                // Response echoes the file back by reference — logged as a file size, never read into memory
                String largeResponseLog = messages.stream()
                        .filter(m -> m.startsWith("Response: POST") && m.contains("/log-test/large"))
                        .findFirst()
                        .orElseThrow();
                assertThat(largeResponseLog).contains(LARGE_FILE_SIZE + " bytes]");

                // Chunked transfer — noted without buffering; endpoint still succeeds
                String chunkedLog = messages.stream()
                        .filter(m -> m.startsWith("Request: POST") && m.contains("/log-test/chunked"))
                        .findFirst()
                        .orElseThrow();
                assertThat(chunkedLog).contains("[multipart: chunked]");
            });

    @Test
    void testLargeMultipartBodyNotBuffered() throws Exception {
        // A sparse file gives us a payload bigger than the test heap without actually writing
        // gigabytes to disk.
        var largeFile = Files.createTempFile("large-multipart", ".bin");
        largeFile.toFile().deleteOnExit();
        try (RandomAccessFile raf = new RandomAccessFile(largeFile.toFile(), "rw")) {
            raf.setLength(LARGE_FILE_SIZE);
        }

        // Only the multipart preamble/epilogue is a literal string; BodyPublishers.ofFile streams
        // the file part straight from disk and, combined with the other known-length publishers,
        // gives the whole request a known Content-Length up front, so the filter can detect the
        // oversize from the header without either side ever buffering the body.
        String boundary = "TestBoundary";
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"large.bin\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n";
        String footer = "\r\n--" + boundary + "--\r\n";

        HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.concat(
                HttpRequest.BodyPublishers.ofString(header, StandardCharsets.UTF_8),
                HttpRequest.BodyPublishers.ofFile(largeFile),
                HttpRequest.BodyPublishers.ofString(footer, StandardCharsets.UTF_8));

        // Force HTTP/1.1: the default client prefers HTTP/2 and may buffer the body while
        // negotiating/falling back, which defeats the whole point of this test.
        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + RestAssured.port + "/log-test/large"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(body)
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        assertThat(response.statusCode()).isEqualTo(200);

        // Drain the response by counting bytes instead of collecting them into an array.
        long responseBytes = 0;
        byte[] buffer = new byte[8192];
        try (InputStream in = response.body()) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                responseBytes += read;
            }
        }
        // Upper bound is the file size plus generous headroom for the multipart envelope
        // (boundary, headers) around the single part — anything past that would be a regression.
        assertThat(responseBytes).isBetween(LARGE_FILE_SIZE, LARGE_FILE_SIZE + 1024);
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
        @Produces(MediaType.MULTIPART_FORM_DATA)
        public MultipartFormDataOutput large(@RestForm FileUpload file) {
            MultipartFormDataOutput output = new MultipartFormDataOutput();
            // Passed by reference (File), not read into memory — the writer streams it from disk.
            output.addFormData("payload", file.uploadedFile().toFile(), MediaType.APPLICATION_OCTET_STREAM_TYPE);
            return output;
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
