package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * Outside dev mode rejections are reported through metrics, JFR and the CDI event, never logged one by one.
 */
public class RejectionNotLoggedOutsideDevModeTest {

    private static final String PREFIX = "quarkus.http.rate-limit.api.";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestHttp.Resource.class))
            .overrideConfigKey(PREFIX + "paths", "/api/*")
            .overrideConfigKey(PREFIX + "phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "request-key", "header:X-Client")
            .overrideRuntimeConfigKey(PREFIX + "permits", "1")
            .overrideRuntimeConfigKey(PREFIX + "window", "1m")
            .overrideRuntimeConfigKey("quarkus.log.category.\"io.quarkus.http.ratelimit\".level", "DEBUG")
            .setLogRecordPredicate(r -> r.getLoggerName().startsWith("io.quarkus.http.ratelimit"))
            .assertLogRecords(records -> assertThat(records)
                    .filteredOn(r -> r.getLevel().intValue() >= Level.FINE.intValue())
                    .noneSatisfy(r -> assertThat(r.getMessage()).contains("rejected key")));

    @Test
    void rejectionIsNotLogged() {
        RestAssured.given().header("X-Client", "alice").get("/api/x").then().statusCode(200);
        RestAssured.given().header("X-Client", "alice").get("/api/x").then().statusCode(429);
    }
}
