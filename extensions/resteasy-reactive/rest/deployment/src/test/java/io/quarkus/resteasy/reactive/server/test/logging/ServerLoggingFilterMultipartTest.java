package io.quarkus.resteasy.reactive.server.test.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.LogRecord;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class ServerLoggingFilterMultipartTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(
                    TestResource.class,
                    TestResource.MultipartInput.class,
                    TestResource.PojoResponse.class))
            .overrideRuntimeConfigKey("quarkus.rest.logging.scope", "request-response")
            .overrideRuntimeConfigKey("quarkus.rest.logging.include-body", "true")
            .overrideRuntimeConfigKey("quarkus.rest.logging.body-limit", "30")
            .setLogRecordPredicate(record -> "io.quarkus.rest.logging".equals(record.getLoggerName()))
            .assertLogRecords(records -> {
                List<String> messages = records.stream().map(LogRecord::getMessage).toList();

                // Multipart request (@MultipartForm POJO)

                String requestLog = messages.stream()
                        .filter(m -> m.startsWith("Request: POST") && m.contains("/log-test/multipart")
                                && !m.contains("/log-test/multipart-direct"))
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

                String responseLog = messages.stream()
                        .filter(m -> m.startsWith("Response: POST") && m.contains("/log-test/multipart")
                                && !m.contains("/log-test/multipart-direct"))
                        .findFirst()
                        .orElseThrow();
                assertThat(responseLog)
                        .contains("Status[200 OK]")
                        .contains("Body:\nAlice:hello multipart");

                // Multipart request (direct @RestForm parameters)

                String directRequestLog = messages.stream()
                        .filter(m -> m.startsWith("Request: POST") && m.contains("/log-test/multipart-direct"))
                        .findFirst()
                        .orElseThrow();
                assertThat(directRequestLog)
                        .contains("Body: Parts[")
                        .contains("name (text/plain): Alice")
                        .contains("message (text/plain): hello multipart")
                        .contains("description (text/plain): this description is intentiona...[truncated]")
                        .contains("image filename=photo.jpg (image/jpeg): [5 bytes]");

                String directResponseLog = messages.stream()
                        .filter(m -> m.startsWith("Response: POST") && m.contains("/log-test/multipart-direct"))
                        .findFirst()
                        .orElseThrow();
                assertThat(directResponseLog)
                        .contains("Status[200 OK]")
                        .contains("Body:\nAlice:hello multipart");

                // Multipart response (MultipartFormDataOutput)

                String outputResponseLog = messages.stream()
                        .filter(m -> m.startsWith("Response: GET") && m.contains("/log-test/multipart-output"))
                        .findFirst()
                        .orElseThrow();
                assertThat(outputResponseLog)
                        .contains("Status[200 OK]")
                        .contains("Body: Parts[")
                        .contains("text (text/plain): hello")
                        .contains("description (text/plain):")
                        .contains("...[truncated]")
                        .contains("data (application/octet-stream): [3 bytes]");

                // Multipart response (@RestForm POJO)

                String pojoResponseLog = messages.stream()
                        .filter(m -> m.startsWith("Response: GET") && m.contains("/log-test/multipart-pojo"))
                        .findFirst()
                        .orElseThrow();
                assertThat(pojoResponseLog)
                        .contains("Status[200 OK]")
                        .contains("Body: Parts[")
                        .contains("text (text/plain): hello pojo")
                        .contains("description (text/plain):")
                        .contains("...[truncated]")
                        .contains("data (application/octet-stream): [4 bytes]");
            });

    @Test
    void testMultipartPojoRequestLogged() throws IOException {
        // RestAssured's multiPart() sends Transfer-Encoding: chunked (no Content-Length).
        // Using MultipartEntityBuilder and sending the result as a byte array forces Content-Length
        // to be set, so the filter buffers and parses the parts.
        String boundary = "TestBoundary";
        HttpEntity entity = MultipartEntityBuilder.create()
                .setBoundary(boundary)
                .addTextBody("name", "Alice", ContentType.TEXT_PLAIN)
                .addTextBody("message", "hello multipart", ContentType.TEXT_PLAIN)
                .addTextBody("description", "this description is intentionally long for testing truncation",
                        ContentType.TEXT_PLAIN)
                .addBinaryBody("image", new byte[] { 1, 2, 3, 4, 5 }, ContentType.create("image/jpeg"), "photo.jpg")
                .build();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        entity.writeTo(out);
        RestAssured.given()
                .contentType("multipart/form-data; boundary=" + boundary)
                .body(out.toByteArray())
                .post("/log-test/multipart")
                .then()
                .statusCode(200);
    }

    @Test
    void testMultipartDirectRequestLogged() throws IOException {
        String boundary = "TestBoundary";
        HttpEntity entity = MultipartEntityBuilder.create()
                .setBoundary(boundary)
                .addTextBody("name", "Alice", ContentType.TEXT_PLAIN)
                .addTextBody("message", "hello multipart", ContentType.TEXT_PLAIN)
                .addTextBody("description", "this description is intentionally long for testing truncation",
                        ContentType.TEXT_PLAIN)
                .addBinaryBody("image", new byte[] { 1, 2, 3, 4, 5 }, ContentType.create("image/jpeg"), "photo.jpg")
                .build();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        entity.writeTo(out);
        RestAssured.given()
                .contentType("multipart/form-data; boundary=" + boundary)
                .body(out.toByteArray())
                .post("/log-test/multipart-direct")
                .then()
                .statusCode(200);
    }

    @Test
    void testMultipartFormDataOutputResponseLogged() {
        RestAssured.get("/log-test/multipart-output")
                .then()
                .statusCode(200);
    }

    @Test
    void testMultipartPojoResponseLogged() {
        RestAssured.get("/log-test/multipart-pojo")
                .then()
                .statusCode(200);
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

        @POST
        @Path("/multipart-direct")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN)
        public String multipartDirect(
                @RestForm @PartType(MediaType.TEXT_PLAIN) String name,
                @RestForm @PartType(MediaType.TEXT_PLAIN) String message,
                @RestForm @PartType(MediaType.TEXT_PLAIN) String description,
                @RestForm FileUpload image) {
            return name + ":" + message;
        }

        @GET
        @Path("/multipart-output")
        @Produces(MediaType.MULTIPART_FORM_DATA)
        public MultipartFormDataOutput multipartOutput() {
            MultipartFormDataOutput output = new MultipartFormDataOutput();
            output.addFormData("text", "hello", MediaType.TEXT_PLAIN_TYPE);
            output.addFormData("description", "this description is intentionally long", MediaType.TEXT_PLAIN_TYPE);
            output.addFormData("data", new byte[] { 1, 2, 3 }, MediaType.APPLICATION_OCTET_STREAM_TYPE);
            return output;
        }

        @GET
        @Path("/multipart-pojo")
        @Produces(MediaType.MULTIPART_FORM_DATA)
        public PojoResponse multipartPojo() {
            PojoResponse response = new PojoResponse();
            response.text = "hello pojo";
            response.description = "this description is intentionally long";
            response.data = new byte[] { 10, 20, 30, 40 };
            return response;
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

        public static class PojoResponse {

            @RestForm
            @PartType(MediaType.TEXT_PLAIN)
            public String text;

            @RestForm
            @PartType(MediaType.TEXT_PLAIN)
            public String description;

            @RestForm
            @PartType(MediaType.APPLICATION_OCTET_STREAM)
            public byte[] data;
        }
    }
}
