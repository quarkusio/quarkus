package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpResponse;

/**
 * Every rule is keyed by a header, with a unique value per test method. {@code abuse} uses its own header: a request
 * without it would be limited by its client address, so every request that does not set it gets a unique value, and
 * {@code abuse} only limits the test that sends the same value twice.
 */
public class StackedRulesTest {

    private static final String PREFIX = "quarkus.http.rate-limit.";

    @RegisterExtension
    static final QuarkusExtensionTest config = withRule(withRule(withRule(withRule(
            new QuarkusExtensionTest()
                    .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class,
                            RejectionObserver.class,
                            TestRateLimitClock.class))
                    .overrideConfigKey("quarkus.vertx.event-loops-pool-size", "1"),
            "abuse", "/*", "header:X-Abuse", 2, "1m"),
            "minute", "/api/*", "header:X-Client", 2, "1m"),
            "hour", "/api/*", "header:X-Client", 3, "1h"),
            "login", "/login", "header:X-Client", 1, "1m")
            .overrideConfigKey(PREFIX + "login.methods", "POST");

    private static QuarkusExtensionTest withRule(QuarkusExtensionTest test, String rule, String path, String key,
            int permits, String window) {
        return test.overrideConfigKey(PREFIX + rule + ".paths", path)
                .overrideConfigKey(PREFIX + rule + ".phase", "pre-authentication")
                .overrideConfigKey(PREFIX + rule + ".request-key", key)
                .overrideRuntimeConfigKey(PREFIX + rule + ".permits", "" + permits)
                .overrideRuntimeConfigKey(PREFIX + rule + ".window", window);
    }

    @Inject
    Vertx vertx;

    @Inject
    TestRateLimitClock clock;

    @Inject
    RejectionObserver observer;

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
    void methodsRestrictARule() {
        Map<String, String> headers = Map.of("X-Client", "methods");
        assertThat(send(HttpMethod.POST, "/login", headers).statusCode()).isEqualTo(200);
        HttpResponse<Buffer> rejected = send(HttpMethod.POST, "/login", headers);
        assertThat(rejected.statusCode()).isEqualTo(429);
        assertThat(observer.lastRule()).isEqualTo("login");
        for (int i = 0; i < 3; i++) {
            assertThat(send(HttpMethod.GET, "/login", headers).statusCode()).isEqualTo(200);
        }
    }

    @Test
    void broadRuleIsEvaluatedTogetherWithNarrowOnes() {
        Map<String, String> headers = Map.of("X-Client", "broad", "X-Abuse", "broad");
        assertThat(send(HttpMethod.GET, "/api/x", headers).statusCode()).isEqualTo(200);
        assertThat(send(HttpMethod.GET, "/api/x", headers).statusCode()).isEqualTo(200);
        HttpResponse<Buffer> rejected = send(HttpMethod.GET, "/other", headers);
        assertThat(rejected.statusCode()).isEqualTo(429);
        assertThat(observer.lastRule()).isEqualTo("abuse");
    }

    @Test
    void narrowestRuleIsEvaluatedFirstAndStopsTheChain() {
        Map<String, String> headers = Map.of("X-Client", "narrow");
        assertThat(send(HttpMethod.GET, "/api/x", headers).statusCode()).isEqualTo(200);
        assertThat(send(HttpMethod.GET, "/api/x", headers).statusCode()).isEqualTo(200);

        HttpResponse<Buffer> third = send(HttpMethod.GET, "/api/x", headers);
        assertThat(third.statusCode()).isEqualTo(429);
        assertThat(observer.lastRule()).isEqualTo("minute");
        assertThat(retryAfter(third)).isEqualTo(30);

        // the minute rejection stopped the chain, so hour still has one permit
        clock.advance(Duration.ofMinutes(1));
        assertThat(send(HttpMethod.GET, "/api/x", headers).statusCode()).isEqualTo(200);

        HttpResponse<Buffer> fifth = send(HttpMethod.GET, "/api/x", headers);
        assertThat(fifth.statusCode()).isEqualTo(429);
        assertThat(observer.lastRule()).isEqualTo("hour");
        // three permits of 1200 s each were consumed at 0 s, 0 s and 60 s
        assertThat(retryAfter(fifth)).isEqualTo(1200 - 60);
    }

    @Test
    void wideRejectionAfterNarrowAllowance() {
        Map<String, String> headers = Map.of("X-Client", "wide");
        for (int i = 0; i < 3; i++) {
            assertThat(send(HttpMethod.GET, "/api/x", headers).statusCode()).isEqualTo(200);
            if (i < 2) {
                clock.advance(Duration.ofMinutes(1));
            }
        }

        // minute allows (one of its two permits is left), hour rejects
        HttpResponse<Buffer> wide = send(HttpMethod.GET, "/api/x", headers);
        assertThat(wide.statusCode()).isEqualTo(429);
        assertThat(observer.lastRule()).isEqualTo("hour");
        assertThat(retryAfter(wide)).isEqualTo(1200 - 120);

        // the documented residual: the minute permit spent on the request hour rejected is gone
        HttpResponse<Buffer> next = send(HttpMethod.GET, "/api/x", headers);
        assertThat(next.statusCode()).isEqualTo(429);
        assertThat(observer.lastRule()).isEqualTo("minute");
        assertThat(retryAfter(next)).isEqualTo(30);
    }

    private HttpResponse<Buffer> send(HttpMethod method, String path, Map<String, String> headers) {
        Map<String, String> all = new HashMap<>(headers);
        all.putIfAbsent("X-Abuse", UUID.randomUUID().toString());
        return http.send(method, path, all, method == HttpMethod.POST ? Buffer.buffer("x")
                : null);
    }

    @Singleton
    public static class RejectionObserver {

        final List<String> rules = new CopyOnWriteArrayList<>();

        void observe(@Observes RateLimitRejectionInfo rejection) {
            rules.add(rejection.rule());
        }

        String lastRule() {
            return rules.getLast();
        }
    }

    private static long retryAfter(HttpResponse<Buffer> response) {
        return Long.parseLong(response.getHeader(HttpHeaders.RETRY_AFTER.toString()));
    }
}
