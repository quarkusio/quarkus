package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class UnknownOverrideFailsBuildTest {

    private static final String PREFIX = "quarkus.http.rate-limit.api.";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey(PREFIX + "paths", "/*")
            .overrideConfigKey(PREFIX + "phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "request-key", "ip")
            .overrideConfigKey(PREFIX + "override", "plans")
            .overrideRuntimeConfigKey(PREFIX + "permits", "1")
            .overrideRuntimeConfigKey(PREFIX + "window", "1m")
            .assertException(t -> assertThat(t).rootCause().isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining(
                            "Rate limit rule 'api' override 'plans' matches no RateLimitOverride bean annotated @Identifier(\"plans\")"));

    @Test
    void buildFails() {
        // the extension asserts the build failure
    }
}
