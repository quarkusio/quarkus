package io.quarkus.http.ratelimit;

import static io.quarkus.http.ratelimit.MetricsScrape.assertScrape;
import static org.assertj.core.api.Assertions.entry;

import java.net.URL;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.http.ratelimit.store.SwitchableTestStore;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;

/**
 * The guide says store health is visible through ft.circuitbreaker.* meters tagged with the guard description.
 */
public class StoreFaultToleranceMetersTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = SwitchableStoreApp
            .rule(SwitchableStoreApp.create("open", MetricsScrape.class), "api", "/*", "switchable")
            .setForcedDependencies(
                    List.of(Dependency.of("io.quarkus", "quarkus-smallrye-fault-tolerance-deployment",
                            Version.getVersion())));

    @Inject
    Vertx vertx;

    @TestHTTPResource
    URL url;

    TestClient http;

    @BeforeEach
    void createClient() {
        http = new TestClient(vertx, url);
    }

    @AfterEach
    void closeClient() {
        http.close();
    }

    @BeforeEach
    void reset() {
        SwitchableTestStore.reset();
    }

    @Test
    void circuitBreakerMetersExist() {
        SwitchableTestStore.behaviour.set(new SwitchableTestStore.Throw());
        for (int i = 0; i < 4; i++) {
            http.get("/x");
        }
        assertScrape(http).hasMetricWithLabels("ft_circuitbreaker_opened_total",
                entry("method", "http-rate-limit-store-switchable"));
    }
}
