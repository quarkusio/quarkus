package io.quarkus.resteasy.reactive.server.test.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class ServerLoggingFilterMultipartTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestResource.class, TestResource.MultipartInput.class))
            .overrideRuntimeConfigKey("quarkus.rest.logging.scope", "request-response")
            .overrideRuntimeConfigKey("quarkus.rest.logging.include-body", "true")
            .overrideRuntimeConfigKey("quarkus.rest.logging.body-limit", "30")
            .setLogRecordPredicate(record -> "io.quarkus.rest.logging".equals(record.getLoggerName()))
            .assertLogRecords(records -> {
                List<String> messages = records.stream().map(LogRecord::getMessage).collect(Collectors.toList());

                // Multipart request is logged with individual parts, not raw bytes or raw content
                String requestLog = messages.stream()
                        .filter(m -> m.startsWith("Request: POST") && m.contains("/log-test/multipart"))
                        .findFirst()
                        .orElseThrow();
                assertThat(requestLog)
                        .contains("Body: Parts[")
                        // short text fields are logged in full
                        .contains("name (text/plain): Alice")
                        .contains("message (text/plain): hello multipart")
                        // long text part is truncated at body-limit
                        .contains("description (text/plain): this description is intentiona...[truncated]")
                        // binary part with filename is logged as byte count, not as content
                        .contains("image filename=photo.jpg (image/jpeg): [5 bytes]")
                        // raw MIME boundary bytes must not be dumped as text
                        .doesNotContain("Body:\n")
                        .doesNotContain("Content-Disposition");

                // Response is a plain text echo — logged as a normal text body, not multipart
                String responseLog = messages.stream()
                        .filter(m -> m.startsWith("Response: POST") && m.contains("/log-test/multipart"))
                        .findFirst()
                        .orElseThrow();
                assertThat(responseLog)
                        .contains("Status[200 OK]")
                        .contains("Body:\nAlice:hello multipart");
            });

    @Test
    void testMultipartRequestLogged() throws IOException {
        // RestAssured's multiPart() uses Apache HttpClient which sends multipart bodies with
        // Transfer-Encoding: chunked (no Content-Length). That would trigger the [multipart: chunked]
        // log path instead of the normal parse path. Building the body manually and sending it as a
        // byte array forces Apache HttpClient to set Content-Length, so the filter buffers and parses.
        String boundary = "TestBoundary";
        byte[] body = buildMultipartBody(boundary);
        RestAssured.given()
                .contentType("multipart/form-data; boundary=" + boundary)
                .body(body)
                .post("/log-test/multipart")
                .then()
                .statusCode(200);
    }

    private static byte[] buildMultipartBody(String boundary) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        addTextPart(out, boundary, "name", "Alice");
        addTextPart(out, boundary, "message", "hello multipart");
        addTextPart(out, boundary, "description", "this description is intentionally long for testing truncation");
        addBinaryPart(out, boundary, "image", "photo.jpg", "image/jpeg", new byte[] { 1, 2, 3, 4, 5 });
        out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    private static void addTextPart(ByteArrayOutputStream out, String boundary, String name, String value)
            throws IOException {
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        out.write("Content-Type: text/plain\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        out.write(value.getBytes(StandardCharsets.UTF_8));
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static void addBinaryPart(ByteArrayOutputStream out, String boundary, String name, String filename,
            String contentType, byte[] data) throws IOException {
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n")
                .getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(data);
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    @Path("/log-test")
    public static class TestResource {

        @POST
        @Path("/multipart")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN)
        public String multipart(@MultipartForm MultipartInput input) {
            return input.name + ":" + input.message;
        }

        public static class MultipartInput {

            @RestForm
            @PartType(MediaType.TEXT_PLAIN)
            public String name;

            @RestForm
            @PartType(MediaType.TEXT_PLAIN)
            public String message;

            @RestForm
            @PartType(MediaType.TEXT_PLAIN)
            public String description;

            @RestForm
            public FileUpload image;
        }
    }
}
