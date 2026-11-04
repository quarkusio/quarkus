package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class StartupLogTest {

    private static final String PREFIX = "quarkus.http.rate-limit.api.";
    private static final String ADMIN = "quarkus.http.rate-limit.admin.";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestHttp.Resource.class))
            .overrideConfigKey(PREFIX + "paths", "/api/*")
            .overrideConfigKey(PREFIX + "phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "request-key", "ip")
            .overrideRuntimeConfigKey(PREFIX + "permits", "5")
            .overrideRuntimeConfigKey(PREFIX + "window", "1m")
            .overrideConfigKey(ADMIN + "paths", "/admin/*")
            .overrideConfigKey(ADMIN + "phase", "pre-authentication")
            .overrideConfigKey(ADMIN + "request-key", "ip")
            .overrideRuntimeConfigKey(ADMIN + "permits", "10")
            .overrideRuntimeConfigKey(ADMIN + "window", "1m")
            .overrideRuntimeConfigKey("quarkus.log.category.\"io.quarkus.http.ratelimit\".level", "DEBUG")
            .setLogRecordPredicate(r -> r.getLoggerName().startsWith("io.quarkus.http.ratelimit"))
            .assertLogRecords(records -> {
                // one INFO line for all rules, not one per rule
                assertThat(records).filteredOn(r -> r.getLevel().intValue() == Level.INFO.intValue()).singleElement()
                        .satisfies(r -> assertThat(format(r)).isEqualTo("Rate limiting 2 rules: admin, api"));
                // the effective configuration of each rule is still available at DEBUG
                assertThat(records).filteredOn(r -> r.getLevel().intValue() == Level.FINE.intValue())
                        .map(StartupLogTest::format)
                        .anySatisfy(m -> assertThat(m).isEqualTo(
                                "Rate limit rule 'api': paths=[/api/*] methods=[] phase=pre-authentication request-key=ip"
                                        + " permits=5 window=PT1M burst=5 max-keys=100000"))
                        .anySatisfy(m -> assertThat(m).startsWith("Rate limit rule 'admin': paths=[/admin/*]"));
            });

    private static String format(LogRecord r) {
        return r.getParameters() == null ? r.getMessage() : String.format(r.getMessage(), r.getParameters());
    }

    @Test
    void oneInfoLineForAllRulesAndDetailsAtDebug() {
    }
}
