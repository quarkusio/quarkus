package io.quarkus.resteasy.reactive.server.test.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class ServerLoggingFilterMultipartOutputTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestResource.class, TestResource.PojoResponse.class))
            .overrideRuntimeConfigKey("quarkus.rest.logging.scope", "request-response")
            .overrideRuntimeConfigKey("quarkus.rest.logging.include-body", "true")
            .overrideRuntimeConfigKey("quarkus.rest.logging.body-limit", "20")
            .setLogRecordPredicate(record -> "io.quarkus.rest.logging".equals(record.getLoggerName()))
            .assertLogRecords(records -> {
                List<String> messages = records.stream().map(LogRecord::getMessage).collect(Collectors.toList());

                // --- MultipartFormDataOutput response ---

                String outputResponseLog = messages.stream()
                        .filter(m -> m.startsWith("Response: GET") && m.contains("/log-test/multipart-output"))
                        .findFirst()
                        .orElseThrow();
                assertThat(outputResponseLog)
                        .contains("Status[200 OK]")
                        .contains("Body: Parts[")
                        // short text part is logged in full
                        .contains("text (text/plain): hello")
                        // long text part is truncated at body-limit
                        .contains("description (text/plain):")
                        .contains("...[truncated]")
                        // binary part is logged as byte count, not content
                        .contains("data (application/octet-stream): [3 bytes]");

                // --- @RestForm POJO response ---

                String pojoResponseLog = messages.stream()
                        .filter(m -> m.startsWith("Response: GET") && m.contains("/log-test/multipart-pojo"))
                        .findFirst()
                        .orElseThrow();
                assertThat(pojoResponseLog)
                        .contains("Status[200 OK]")
                        .contains("Body: Parts[")
                        // short text field is logged in full
                        .contains("text (text/plain): hello pojo")
                        // long text field is truncated at body-limit
                        .contains("description (text/plain):")
                        .contains("...[truncated]")
                        // binary field is logged as byte count
                        .contains("data (application/octet-stream): [4 bytes]");
            });

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
