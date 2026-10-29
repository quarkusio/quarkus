package io.quarkus.resteasy.reactive.server.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * The constraint violations of a request rejected with 400 are logged at DEBUG level by the builtin exception mapper,
 * so that they can be seen in the console by enabling the category.
 */
public class ViolationReportDebugLogTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(GreetingResource.class))
            .overrideConfigKey("quarkus.log.category.\"io.quarkus.hibernate.validator\".level", "DEBUG")
            .setLogRecordPredicate(record -> record.getLevel().intValue() <= Level.FINE.intValue()
                    && record.getLoggerName().startsWith("io.quarkus.hibernate.validator"))
            .assertLogRecords(records -> assertThat(records).map(ViolationReportDebugLogTest::format)
                    .anyMatch(message -> message.contains("400") && message.contains("greet.name")
                            && message.contains("must not be null")));

    @Test
    public void testViolationIsReportedAndLogged() {
        RestAssured.when().get("/greet")
                .then()
                .statusCode(400);
        RestAssured.when().get("/greet?name=Quarkus")
                .then()
                .statusCode(200);
    }

    private static String format(LogRecord record) {
        Object[] parameters = record.getParameters();
        if (parameters == null || parameters.length == 0) {
            return record.getMessage();
        }
        return String.format(record.getMessage(), parameters);
    }

    @Path("/greet")
    public static class GreetingResource {

        @GET
        public String greet(@NotNull @QueryParam("name") String name) {
            return "Hello " + name;
        }
    }
}
