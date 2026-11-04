package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.http.ratelimit.runtime.internal.keys.RateLimitKeys;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

/**
 * In dev mode every rejection is logged at INFO with its key, so that a developer sees why a request got 429.
 */
public class DevModeRejectionLogTest {

    // not 8080, which a developer's own dev mode session may hold
    private static final int PORT = 18089;

    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestHttp.Resource.class)
                    .addAsResource(new StringAsset("""
                            quarkus.http.port=18089
                            quarkus.http.rate-limit.api.paths=/api/*
                            quarkus.http.rate-limit.api.phase=pre-authentication
                            quarkus.http.rate-limit.api.request-key=header:X-Client
                            quarkus.http.rate-limit.api.permits=1
                            quarkus.http.rate-limit.api.window=1m
                            """), "application.properties"))
            .setLogRecordPredicate(r -> r.getLoggerName().startsWith("io.quarkus.http.ratelimit"));

    @Test
    void rejectionsAreLoggedAtInfo() {
        RestAssured.given().port(PORT).header("X-Client", "alice").get("/api/x").then().statusCode(200);
        RestAssured.given().port(PORT).header("X-Client", "alice").get("/api/x").then().statusCode(429);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            List<String> info = test.getLogRecords().stream()
                    .filter(r -> r.getLevel().intValue() == Level.INFO.intValue())
                    .map(DevModeRejectionLogTest::format).toList();
            assertThat(info).anySatisfy(m -> assertThat(m)
                    .startsWith("Rate limit rule 'api' rejected key 'header:" + RateLimitKeys.sha256("alice") + "'")
                    .contains("Retry-After"));
        });
    }

    private static String format(LogRecord r) {
        return r.getParameters() == null ? r.getMessage() : String.format(r.getMessage(), r.getParameters());
    }
}
