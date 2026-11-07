package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.http.ratelimit.store.SwitchableTestStore;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * The global settings apply to the rules that do not set their own: {@code api} takes all of them, {@code admin} sets
 * its own. {@link HttpRateLimitConfigValidatorTest} covers the settings that the startup log does not show.
 */
public class GlobalDefaultsTest {

    private static final String ROOT = "quarkus.http.rate-limit.";
    private static final String API = "quarkus.http.rate-limit.api.";
    private static final String ADMIN = "quarkus.http.rate-limit.admin.";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestHttp.Resource.class,
                    SwitchableTestStore.class))
            // build time
            .overrideConfigKey(ROOT + "phase", "pre-authentication")
            .overrideConfigKey(ROOT + "request-key", "header:X-Client")
            .overrideConfigKey(ROOT + "store", "switchable")
            // runtime
            .overrideRuntimeConfigKey(ROOT + "max-keys", "500")
            .overrideConfigKey(API + "paths", "/api/*")
            .overrideRuntimeConfigKey(API + "permits", "50")
            .overrideRuntimeConfigKey(API + "window", "1m")
            .overrideConfigKey(ADMIN + "paths", "/admin/*")
            .overrideConfigKey(ADMIN + "phase", "post-authentication")
            .overrideConfigKey(ADMIN + "request-key", "ip")
            .overrideConfigKey(ADMIN + "store", "local")
            .overrideRuntimeConfigKey(ADMIN + "permits", "10")
            .overrideRuntimeConfigKey(ADMIN + "window", "1m")
            .overrideRuntimeConfigKey(ADMIN + "max-keys", "20")
            .overrideRuntimeConfigKey("quarkus.log.category.\"io.quarkus.http.ratelimit\".level", "DEBUG")
            .setLogRecordPredicate(r -> r.getLoggerName().startsWith("io.quarkus.http.ratelimit"))
            .assertLogRecords(records -> assertThat(records)
                    .filteredOn(r -> r.getLevel().intValue() == Level.FINE.intValue())
                    .map(GlobalDefaultsTest::format)
                    .anySatisfy(m -> assertThat(m).isEqualTo(
                            "Rate limit rule 'api': paths=[/api/*] methods=[] phase=pre-authentication"
                                    + " request-key=header:X-Client permits=50 window=PT1M burst=50 store=switchable"
                                    + " lease-size=- max-keys=500"))
                    .anySatisfy(m -> assertThat(m).isEqualTo(
                            "Rate limit rule 'admin': paths=[/admin/*] methods=[] phase=post-authentication request-key=ip"
                                    + " permits=10 window=PT1M burst=10 store=local lease-size=- max-keys=20")));

    private static String format(LogRecord r) {
        return r.getParameters() == null ? r.getMessage() : String.format(r.getMessage(), r.getParameters());
    }

    @Test
    void rulesTakeTheGlobalSettingsTheyDoNotSet() {
    }
}
