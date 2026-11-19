package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.logging.Level;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.faulttolerance.api.Guard;
import io.vertx.core.Vertx;

/**
 * A customizer's own {@code skipOn} adds to the extension's: neither its exceptions nor pool refusals open the
 * breaker, and other failures still do.
 */
public class PoolRefusalWithSkipOnCustomizerTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = SwitchableStoreApp.rule(SwitchableStoreApp.create("open",
            PoolRefusalTest.class, SkipIgnored.class),
            "api", "/*", "switchable")
            .assertLogRecords(records -> assertThat(SwitchableStoreApp.count(records, Level.WARNING, "is degraded"))
                    .isEqualTo(1));

    @Inject
    Vertx vertx;

    @TestHTTPResource
    URL url;

    @Test
    void customizerExceptionsAndPoolRefusalsDoNotOpenTheBreaker() {
        PoolRefusalTest.assertSkippedThenOpens(vertx, url);
    }

    @Singleton
    public static class SkipIgnored implements RateLimitStoreGuardCustomizer {

        @Override
        public void customize(String storeName, Guard.Builder.CircuitBreakerBuilder breaker) {
            breaker.skipOn(PoolRefusalTest.Ignored.class);
        }
    }
}
