package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A limit set at runtime for a rule that the build does not know, for example because of a typo in its name.
 */
public class RuleNotDefinedAtBuildTimeFailsStartupTest {

    private static final String PREFIX = "quarkus.http.rate-limit.api.";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey(PREFIX + "paths", "/*")
            .overrideConfigKey(PREFIX + "phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "request-key", "ip")
            .overrideRuntimeConfigKey(PREFIX + "permits", "1")
            .overrideRuntimeConfigKey(PREFIX + "window", "1m")
            .overrideRuntimeConfigKey("quarkus.http.rate-limit.apj.permits", "1")
            .overrideRuntimeConfigKey("quarkus.http.rate-limit.apj.window", "1m")
            .assertException(t -> assertThat(t).isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("Rate limit rule 'apj' is not defined at build time: its paths"
                            + " (quarkus.http.rate-limit.apj.paths) must be set when the application is built"));

    @Test
    void startupFails() {
        // the extension asserts the startup failure
    }
}
