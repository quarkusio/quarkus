package io.quarkus.resteasy.reactive.server.test.logging;

import static org.assertj.core.api.Assertions.assertThat;

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
 Verifies the large-payload behavior with the default body-limit (100). Buffering a multipart
 request for logging is capped at min(max(body-limit, an internal floor), an internal ceiling), so
 with the default body-limit the effective threshold is the floor — well under this test's payload.
 The payload (2 GiB) is deliberately bigger than the test JVM's heap (build-parent sets surefire's
 argLine to -Xmx1500m), so if this feature ever regressed into buffering the body fully in memory,
 the test would fail with an OutOfMemoryError instead of silently passing. The request is streamed
 from a sparse file (so no gigabytes are actually written to disk) and the response is drained by
 counting bytes rather than collecting them, so nothing on either side ever holds the whole body at
 once.
*/
public class ServerLoggingFilterMultipartDefaultBufferLimitTest {

    private static final long LARGE_FILE_SIZE = 2L * 1024 * 1024 * 1024; // 2 GiB — bigger than the test JVM's heap

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestResource.class))
            .overrideRuntimeConfigKey("quarkus.rest.logging.scope", "request-response")
            .overrideRuntimeConfigKey("quarkus.rest.logging.include-body", "true")
            .overrideRuntimeConfigKey("quarkus.http.limits.max-body-size", "3G") // default 10240K rejects our 2 GiB payload
            .setLogRecordPredicate(record -> "io.quarkus.rest.logging".equals(record.getLoggerName()))
            .assertLogRecords(records -> {
                List<String> messages = records.stream().map(LogRecord::getMessage).toList();

                // Request body exceeds the default effective buffering threshold — noted without buffering
                String largeLog = messages.stream()
                        .filter(m -> m.startsWith("Request: POST") && m.contains("/log-test/large-default"))
                        .findFirst()
                        .orElseThrow();
                assertThat(largeLog).contains("bytes, body too large to log");

                // Response echoes the file back by reference — logged as a file size, never read into memory
                String largeResponseLog = messages.stream()
                        .filter(m -> m.startsWith("Response: POST") && m.contains("/log-test/large-default"))
                        .findFirst()
                        .orElseThrow();
                assertThat(largeResponseLog).contains(LARGE_FILE_SIZE + " bytes]");
            });

    @Test
    void testLargeMultipartBodyNotBufferedWithDefaultLimit() throws Exception {
        // A sparse file gives us a payload bigger than the test heap without actually writing
        // gigabytes to disk.
        var largeFile = Files.createTempFile("large-multipart-default", ".bin");
        largeFile.toFile().deleteOnExit();
        try (RandomAccessFile raf = new RandomAccessFile(largeFile.toFile(), "rw")) {
            raf.setLength(LARGE_FILE_SIZE);
        }

        // Only the multipart preamble/epilogue is a literal string; BodyPublishers.ofFile streams
        // the file part straight from disk and, combined with the other known-length publishers,
        // gives the whole request a known Content-Length up front, so nothing here is buffered.
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
                .uri(URI.create("http://localhost:" + RestAssured.port + "/log-test/large-default"))
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

    @Path("/log-test")
    public static class TestResource {

        @POST
        @Path("/large-default")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.MULTIPART_FORM_DATA)
        public MultipartFormDataOutput large(@RestForm FileUpload file) {
            MultipartFormDataOutput output = new MultipartFormDataOutput();
            // Passed by reference (File), not read into memory — the writer streams it from disk.
            output.addFormData("payload", file.uploadedFile().toFile(), MediaType.APPLICATION_OCTET_STREAM_TYPE);
            return output;
        }
    }
}
