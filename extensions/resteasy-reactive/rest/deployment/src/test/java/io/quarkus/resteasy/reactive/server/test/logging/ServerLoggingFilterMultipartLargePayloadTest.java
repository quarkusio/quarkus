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
import java.nio.file.Files;
import java.util.List;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/* Verifies that incoming multipart requests whose Content-Length exceeds bodyBufferLimit are
  noted in the log without buffering the body into memory.
 */
public class ServerLoggingFilterMultipartLargePayloadTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestResource.class))
            .overrideRuntimeConfigKey("quarkus.rest.logging.scope", "request-response")
            .overrideRuntimeConfigKey("quarkus.rest.logging.include-body", "true")
            .overrideRuntimeConfigKey("quarkus.rest.logging.body-buffer-limit", "1k") // test bodies are 2 kB — over the limit
            .setLogRecordPredicate(record -> "io.quarkus.rest.logging".equals(record.getLoggerName()))
            .assertLogRecords(records -> {
                List<String> messages = records.stream().map(LogRecord::getMessage).collect(Collectors.toList());

                // Request body larger than bodyBufferLimit — noted without buffering; endpoint still succeeds
                String largeLog = messages.stream()
                        .filter(m -> m.startsWith("Request: POST") && m.contains("/log-test/large"))
                        .findFirst()
                        .orElseThrow();
                assertThat(largeLog).contains("bytes, body too large to log");

                // Response echoes the file back — large binary part logged as byte count, not inlined
                String largeResponseLog = messages.stream()
                        .filter(m -> m.startsWith("Response: POST") && m.contains("/log-test/large"))
                        .findFirst()
                        .orElseThrow();
                assertThat(largeResponseLog).contains("payload (application/octet-stream): [2048 bytes]");

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
        // Transfer-Encoding: chunked (no Content-Length). Using MultipartEntityBuilder and sending
        // the result as a byte array forces Content-Length to be set, so the filter can detect the
        // oversize from the header without touching the stream.
        String boundary = "TestBoundary";
        HttpEntity entity = MultipartEntityBuilder.create()
                .setBoundary(boundary)
                .addBinaryBody("file", new byte[2048], ContentType.APPLICATION_OCTET_STREAM, "large.bin")
                .build();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        entity.writeTo(out);
        byte[] body = out.toByteArray();

        byte[] responseBody = RestAssured.given()
                .contentType("multipart/form-data; boundary=" + boundary)
                .body(body)
                .post("/log-test/large")
                .then()
                .statusCode(200)
                .extract().asByteArray();
        assertThat(responseBody.length).isGreaterThan(2048);
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
        public MultipartFormDataOutput large(@RestForm FileUpload file) throws IOException {
            MultipartFormDataOutput output = new MultipartFormDataOutput();
            output.addFormData("payload", Files.readAllBytes(file.uploadedFile()), MediaType.APPLICATION_OCTET_STREAM_TYPE);
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
