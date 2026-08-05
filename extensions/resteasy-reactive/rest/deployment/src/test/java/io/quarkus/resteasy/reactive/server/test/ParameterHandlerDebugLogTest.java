package io.quarkus.resteasy.reactive.server.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;

import org.jboss.logmanager.Level;
import org.jboss.resteasy.reactive.server.handlers.ParameterHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * Verifies that parameter conversion errors are logged at DEBUG level in dev/test mode,
 * so that the root cause of HTTP 400 responses is visible without manual log configuration.
 */
public class ParameterHandlerDebugLogTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestResource.class))
            .setLogRecordPredicate(record -> record.getLoggerName().equals(ParameterHandler.class.getName())
                    && record.getLevel().equals(Level.DEBUG))
            .assertLogRecords(records -> {
                var messages = records.stream().map(LogRecord::getMessage).collect(Collectors.toList());
                assertThat(messages).anySatisfy(msg -> assertThat(msg).contains("Unable to handle parameter"));
            });

    @Test
    public void testBadHeaderParamLogsDebug() {
        RestAssured
                .given()
                .header("X-Number", "not-a-number")
                .get("/test/header")
                .then()
                .statusCode(400);
    }

    @Path("/test")
    public static class TestResource {

        @GET
        @Path("/header")
        public String header(@HeaderParam("X-Number") int number) {
            return "number=" + number;
        }
    }
}
