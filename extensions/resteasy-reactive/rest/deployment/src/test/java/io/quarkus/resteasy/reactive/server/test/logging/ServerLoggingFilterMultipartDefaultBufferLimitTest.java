package io.quarkus.resteasy.reactive.server.test.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.LogRecord;

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

/*
 Verifies the large-payload behavior with the default body-buffer-limit (10k).
*/
public class ServerLoggingFilterMultipartDefaultBufferLimitTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestResource.class))
            .overrideRuntimeConfigKey("quarkus.rest.logging.scope", "request-response")
            .overrideRuntimeConfigKey("quarkus.rest.logging.include-body", "true")
            .setLogRecordPredicate(record -> "io.quarkus.rest.logging".equals(record.getLoggerName()))
            .assertLogRecords(records -> {
                List<String> messages = records.stream().map(LogRecord::getMessage).toList();

                // Request body exceeds default 10k limit — noted without buffering
                String largeLog = messages.stream()
                        .filter(m -> m.startsWith("Request: POST") && m.contains("/log-test/large-default"))
                        .findFirst()
                        .orElseThrow();
                assertThat(largeLog).contains("bytes, body too large to log");

                // Response echoes the file back — large binary part logged as byte count, not inlined
                String largeResponseLog = messages.stream()
                        .filter(m -> m.startsWith("Response: POST") && m.contains("/log-test/large-default"))
                        .findFirst()
                        .orElseThrow();
                assertThat(largeResponseLog).contains("payload (application/octet-stream): [15360 bytes]");
            });

    @Test
    void testLargeMultipartBodyNotBufferedWithDefaultLimit() throws IOException {
        String boundary = "TestBoundary";
        HttpEntity entity = MultipartEntityBuilder.create()
                .setBoundary(boundary)
                .addBinaryBody("file", new byte[15 * 1024], ContentType.APPLICATION_OCTET_STREAM, "large.bin")
                .build();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        entity.writeTo(out);
        byte[] body = out.toByteArray();

        byte[] responseBody = RestAssured.given()
                .contentType("multipart/form-data; boundary=" + boundary)
                .body(body)
                .post("/log-test/large-default")
                .then()
                .statusCode(200)
                .extract().asByteArray();
        assertThat(responseBody.length).isGreaterThan(15 * 1024);
    }

    @Path("/log-test")
    public static class TestResource {

        @POST
        @Path("/large-default")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.MULTIPART_FORM_DATA)
        public MultipartFormDataOutput large(@RestForm FileUpload file) throws IOException {
            MultipartFormDataOutput output = new MultipartFormDataOutput();
            output.addFormData("payload", Files.readAllBytes(file.uploadedFile()), MediaType.APPLICATION_OCTET_STREAM_TYPE);
            return output;
        }
    }
}
