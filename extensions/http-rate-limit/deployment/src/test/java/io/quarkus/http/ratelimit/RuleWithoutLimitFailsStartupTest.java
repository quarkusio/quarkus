package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A rule defined at build time needs its limit at runtime.
 */
public class RuleWithoutLimitFailsStartupTest {

    private static final String PREFIX = "quarkus.http.rate-limit.api.";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey(PREFIX + "paths", "/*")
            .overrideConfigKey(PREFIX + "phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "request-key", "ip")
            .assertException(t -> assertThat(t).isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("Rate limit rule 'api' has no limit: set quarkus.http.rate-limit.api.permits"
                            + " and quarkus.http.rate-limit.api.window"));

    @Test
    void startupFails() {
        // the extension asserts the startup failure
    }
}
