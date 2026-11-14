package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class UnsupportedPathKeyFailsBuildTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.http.rate-limit.rules.api.paths", "/api/*")
            .overrideConfigKey("quarkus.http.rate-limit.rules.api.phase", "pre-authentication")
            .overrideConfigKey("quarkus.http.rate-limit.rules.api.request-key", "path")
            .overrideRuntimeConfigKey("quarkus.http.rate-limit.rules.api.permits", "1")
            .overrideRuntimeConfigKey("quarkus.http.rate-limit.rules.api.window", "1m")
            .assertException(t -> assertThat(t).rootCause().isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("Rate limit rule 'api' request-key 'path' is not a built-in key"
                            + " (ip, global, principal), header:<name> or custom:<identifier>"));

    @Test
    void buildFails() {
        // the extension asserts the build failure
    }
}
