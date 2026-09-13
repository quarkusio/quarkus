package io.quarkus.resteasy.reactive.server.test;

import static io.restassured.RestAssured.when;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class NoLegacyJaxRsAnnotationsWarningTest {

    private static final String LOGGER = "io.quarkus.resteasy.reactive.server.deployment.LegacyJaxRsAnnotationsProcessor";

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(HelloResource.class))
            .setLogRecordPredicate(record -> LOGGER.equals(record.getLoggerName()))
            .assertLogRecords(records -> assertThat(records).isEmpty());

    @Test
    public void noWarningForJakartaAnnotations() {
        when().get("/hello").then().statusCode(200);
    }

    @Path("hello")
    public static class HelloResource {

        @GET
        public String hello() {
            return "hello";
        }
    }
}
