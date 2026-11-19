package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.logging.Level;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.http.ratelimit.PoolRefusalTest.Ignored;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.faulttolerance.api.Guard;
import io.vertx.core.Vertx;

/**
 * A customizer's {@code when} predicate, which SmallRye Fault Tolerance does not let combine with {@code skipOn}, still
 * leaves pool refusals out of the breaker.
 */
public class PoolRefusalWithWhenCustomizerTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = SwitchableStoreApp.rule(SwitchableStoreApp.create("open",
            PoolRefusalTest.class, FailUnlessIgnored.class),
            "api", "/*", "switchable")
            .assertLogRecords(records -> assertThat(SwitchableStoreApp.count(records, Level.WARNING, "is degraded"))
                    .isEqualTo(1));

    @Inject
    Vertx vertx;

    @TestHTTPResource
    URL url;

    @Test
    void customizerPredicateAndPoolRefusalsDoNotOpenTheBreaker() {
        PoolRefusalTest.assertSkippedThenOpens(vertx, url);
    }

    @Singleton
    public static class FailUnlessIgnored implements RateLimitStoreGuardCustomizer {

        @Override
        public void customize(String storeName, Guard.Builder.CircuitBreakerBuilder breaker) {
            breaker.when(failure -> !(failure instanceof Ignored));
        }
    }
}
