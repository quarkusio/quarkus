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
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class ServerLoggingFilterBodyDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestResource.class))
            .overrideRuntimeConfigKey("quarkus.rest.logging.scope", "request-response")
            // include-body defaults to false — body must not appear in any log message
            .setLogRecordPredicate(record -> "io.quarkus.rest.logging".equals(record.getLoggerName()))
            .assertLogRecords(records -> {
                List<String> messages = records.stream().map(LogRecord::getMessage).collect(Collectors.toList());

                // Requests and responses ARE logged (scope is request-response)
                assertThat(messages).isNotEmpty();

                // But no Body lines appear in any log message
                for (String message : messages) {
                    assertThat(message).doesNotContain("Body:");
                    assertThat(message).doesNotContain("Empty body");
                }
            });

    @Test
    void testGetBodyNotLogged() {
        RestAssured.get("/log-body-disabled/simple")
                .then()
                .statusCode(200);
    }

    @Test
    void testPostBodyNotLogged() {
        RestAssured.given()
                .contentType(MediaType.TEXT_PLAIN)
                .body("sensitive data that must not be logged")
                .post("/log-body-disabled/echo")
                .then()
                .statusCode(200);
    }

    @Path("/log-body-disabled")
    public static class TestResource {

        @GET
        @Path("/simple")
        @Produces(MediaType.TEXT_PLAIN)
        public String simple() {
            return "hello";
        }

        @POST
        @Path("/echo")
        @Consumes(MediaType.TEXT_PLAIN)
        @Produces(MediaType.TEXT_PLAIN)
        public String echo(String body) {
            return body;
        }
    }
}
