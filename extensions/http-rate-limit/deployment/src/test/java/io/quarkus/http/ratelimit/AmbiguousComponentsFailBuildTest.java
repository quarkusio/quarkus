package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Optional;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;

public class AmbiguousComponentsFailBuildTest {
    private static final String RULE = "quarkus.http.rate-limit.rules.api.";

    @RegisterExtension
    static final QuarkusExtensionTest app = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(Components.class))
            .overrideConfigKey(RULE + "paths", "/*")
            .overrideConfigKey(RULE + "phase", "pre-authentication")
            .overrideConfigKey(RULE + "request-key", "custom:tenant")
            .overrideConfigKey(RULE + "limit-resolver", "plan")
            .overrideConfigKey(RULE + "store", "shared")
            .assertException(t -> assertThat(t).rootCause().isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining(RULE + "request-key", RULE + "limit-resolver", RULE + "store",
                            "ambiguous", "firstKey", "secondKey", "firstLimit", "secondLimit", "firstStore", "secondStore"));

    @Test
    void buildFailsWithThePropertiesAndMatchingBeans() {
        // The extension asserts that all ambiguous selections are reported together during augmentation.
    }

    @Singleton
    public static class Components {
        @Produces
        @Identifier("tenant")
        RateLimitKeyResolver firstKey() {
            return ctx -> Uni.createFrom().item(Optional.of("tenant"));
        }

        @Produces
        @Identifier("tenant")
        RateLimitKeyResolver secondKey() {
            return firstKey();
        }

        @Produces
        @Identifier("plan")
        RateLimitResolver firstLimit() {
            return ctx -> Uni.createFrom().item(RateLimit.of(1, Duration.ofMinutes(1)));
        }

        @Produces
        @Identifier("plan")
        RateLimitResolver secondLimit() {
            return firstLimit();
        }

        @Produces
        @Identifier("shared")
        RateLimitStore firstStore() {
            return (key, cost, policy) -> Uni.createFrom().item(RateLimitDecision.allowed(0));
        }

        @Produces
        @Identifier("shared")
        RateLimitStore secondStore() {
            return firstStore();
        }
    }
}
