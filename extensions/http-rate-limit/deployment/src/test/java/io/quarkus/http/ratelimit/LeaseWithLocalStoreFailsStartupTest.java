package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Lease mode needs a shared store; the validator's other lease bounds (lease-size, max-overdraft) are
 * covered by its unit tests.
 */
public class LeaseWithLocalStoreFailsStartupTest {

    private static final String PREFIX = "quarkus.http.rate-limit.api.";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey(PREFIX + "paths", "/*")
            .overrideConfigKey(PREFIX + "phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "request-key", "ip")
            .overrideRuntimeConfigKey(PREFIX + "permits", "10")
            .overrideRuntimeConfigKey(PREFIX + "window", "1m")
            .overrideRuntimeConfigKey(PREFIX + "lease-size", "2")
            .assertException(t -> assertThat(t).isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("Rate limit rule 'api' lease-size requires a non-local store"));

    @Test
    void startupFails() {
        // the extension asserts the startup failure
    }
}
