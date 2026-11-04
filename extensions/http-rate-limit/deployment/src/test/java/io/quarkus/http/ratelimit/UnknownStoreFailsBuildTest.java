package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class UnknownStoreFailsBuildTest {

    private static final String PREFIX = "quarkus.http.rate-limit.rules.api.";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey(PREFIX + "paths", "/*")
            .overrideConfigKey(PREFIX + "phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "request-key", "ip")
            .overrideRuntimeConfigKey(PREFIX + "permits", "1")
            .overrideRuntimeConfigKey(PREFIX + "window", "1m")
            .overrideConfigKey(PREFIX + "store", "nope")
            .assertException(t -> assertThat(t).rootCause().isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("Rate limit rule 'api' store 'nope' matches no RateLimitStore bean annotated"
                            + " @Identifier(\"nope\"); the built-in 'redis' store requires the quarkus-redis-client"
                            + " extension"));

    @Test
    void buildFails() {
        // the extension asserts the build failure
    }
}
