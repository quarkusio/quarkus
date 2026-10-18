package io.quarkus.funqy.test;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

public class NoInputPostWarningTest {
    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(PrimitiveFunctions.class))
            .setLogRecordPredicate(logRecord -> logRecord.getLevel().intValue() >= Level.WARNING.intValue()
                    && logRecord.getLoggerName().startsWith("io.quarkus.funqy"))
            .assertLogRecords(logRecords -> {
                boolean match = logRecords.stream()
                        .anyMatch(logRecord -> logRecord.getMessage().contains("does not accept input"));
                if (!match) {
                    fail("Expected warning about ignored request body was not logged.");
                }
            });

    @Test
    public void testPostWithBodyToNoInputFunction() {
        RestAssured.given().contentType("application/json")
                .body("{}")
                .post("/noop")
                .then().statusCode(204);
    }
}
