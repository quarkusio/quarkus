package io.quarkus.resteasy.reactive.server.test.logging;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class ServerLoggingFilterDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestResource.class))
            // scope defaults to "none" — no logging expected
            .setLogRecordPredicate(record -> "io.quarkus.rest.logging".equals(record.getLoggerName()))
            .assertLogRecords(records -> assertThat(records).isEmpty());

    @Test
    void testNoLoggingWhenScopeIsNone() {
        RestAssured.get("/log-test/simple")
                .then()
                .statusCode(200);
    }

    @Path("/log-test")
    public static class TestResource {

        @GET
        @Path("/simple")
        @Produces(MediaType.TEXT_PLAIN)
        public String simple() {
            return "hello";
        }
    }
}
