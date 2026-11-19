package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class UnknownDefaultStoreFailsBuildTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = TestHttp.ipRule(new QuarkusExtensionTest(), "api", "/*", 1, "1m")
            .overrideConfigKey("quarkus.http.rate-limit.store", "nope")
            .assertException(t -> assertThat(t).rootCause().isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("Rate limit rule 'api' store 'nope' matches no"
                            + " RateLimitStore bean annotated @Identifier(\"nope\"); the built-in 'redis' store"
                            + " requires the quarkus-redis-client extension"));

    @Test
    void buildFails() {
        // the extension asserts the build failure
    }
}
