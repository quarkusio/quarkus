package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;

/**
 * A local store customizer is called once for every cache of each rule, at startup, and what it sets takes effect.
 */
public class LocalStoreCustomizerTest {

    static final List<String> RULES = new CopyOnWriteArrayList<>();
    static final Map<String, AtomicLong> ACTIVITY = new ConcurrentHashMap<>();

    @RegisterExtension
    static final QuarkusExtensionTest config = withRule(withRule(new QuarkusExtensionTest()
            .withApplicationRoot(
                    jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class, RecordingStats.class)),
            "a"), "b");

    private static QuarkusExtensionTest withRule(QuarkusExtensionTest test, String rule) {
        return TestHttp.ipRule(test, rule, "/" + rule + "/*", 10, "1m");
    }

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

    @Test
    void calledOncePerRuleCacheAndTakesEffect() {
        assertThat(RULES).containsExactlyInAnyOrder("a", "b");
        for (int i = 0; i < 3; i++) {
            assertThat(http.get("/a/x").statusCode()).isEqualTo(200);
        }
        assertThat(ACTIVITY.get("a")).hasPositiveValue();
        assertThat(ACTIVITY.get("b")).hasValue(0);
    }

    @Singleton
    public static class RecordingStats implements RateLimitLocalStoreCustomizer {

        @Override
        public void customize(String rule, Settings settings) {
            RULES.add(rule);
            AtomicLong activity = ACTIVITY.computeIfAbsent(rule, r -> new AtomicLong());
            settings.statistics(new Statistics() {
                @Override
                public void hits(int count) {
                    activity.addAndGet(count);
                }

                @Override
                public void misses(int count) {
                    activity.addAndGet(count);
                }

                @Override
                public void updated() {
                    activity.incrementAndGet();
                }
            });
        }
    }
}
