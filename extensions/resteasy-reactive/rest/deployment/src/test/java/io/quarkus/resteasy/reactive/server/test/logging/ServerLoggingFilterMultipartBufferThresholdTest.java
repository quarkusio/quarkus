package io.quarkus.resteasy.reactive.server.test.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.LogRecord;

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

/*
 Verifies that a body-limit strictly between the internal floor (1 KiB) and ceiling (10 KiB) is used
 as the multipart buffering threshold as-is, not just clamped to one of the bounds: a request whose
 Content-Length is just under the configured body-limit is buffered and parsed, one just over it is
 not.
*/
public class ServerLoggingFilterMultipartBufferThresholdTest {

    private static final int BODY_LIMIT = 2000;
    // Margin comfortably larger than the multipart part's own header/boundary overhead, so the
    // resulting Content-Length lands clearly under/over BODY_LIMIT regardless of that overhead.
    private static final int MARGIN = 300;

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestResource.class))
            .overrideRuntimeConfigKey("quarkus.rest.logging.scope", "request-response")
            .overrideRuntimeConfigKey("quarkus.rest.logging.include-body", "true")
            .overrideRuntimeConfigKey("quarkus.rest.logging.body-limit", String.valueOf(BODY_LIMIT))
            .setLogRecordPredicate(record -> "io.quarkus.rest.logging".equals(record.getLoggerName()))
            .assertLogRecords(records -> {
                List<String> messages = records.stream().map(LogRecord::getMessage).toList();

                String underLog = messages.stream()
                        .filter(m -> m.startsWith("Request: POST") && m.contains("/log-test/under"))
                        .findFirst()
                        .orElseThrow();
                assertThat(underLog).contains("Body: Parts[");

                String overLog = messages.stream()
                        .filter(m -> m.startsWith("Request: POST") && m.contains("/log-test/over"))
                        .findFirst()
                        .orElseThrow();
                assertThat(overLog).contains("bytes, body too large to log");
            });

    @Test
    void testBodyJustUnderCustomLimitIsBuffered() throws IOException {
        post("/log-test/under", BODY_LIMIT - MARGIN);
    }

    @Test
    void testBodyJustOverCustomLimitIsNotBuffered() throws IOException {
        post("/log-test/over", BODY_LIMIT + MARGIN);
    }

    private void post(String path, int filePartSize) throws IOException {
        String boundary = "TestBoundary";
        RestAssured.given()
                .contentType("multipart/form-data; boundary=" + boundary)
                .body(multipartBody(boundary, filePartSize))
                .post(path)
                .then()
                .statusCode(200);
    }

    private static byte[] multipartBody(String boundary, int filePartSize) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"data.bin\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(new byte[filePartSize]);
        out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    @Path("/log-test")
    public static class TestResource {

        @POST
        @Path("/under")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN)
        public String under(@RestForm FileUpload file) {
            return "ok";
        }

        @POST
        @Path("/over")
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.TEXT_PLAIN)
        public String over(@RestForm FileUpload file) {
            return "ok";
        }
    }
}
