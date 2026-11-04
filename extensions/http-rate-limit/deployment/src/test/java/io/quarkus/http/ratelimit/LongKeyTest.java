package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

/**
 * A key longer than 256 characters is replaced by its SHA-256 before it reaches the store, so that {@code max-keys}
 * bounds the memory of the store; equal long keys still share a bucket. A test resolver supplies the raw header value
 * because a built-in {@code header:} key holds the hash of the value already.
 */
public class LongKeyTest {

    private static final String PREFIX = "quarkus.http.rate-limit.api.";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class,
                    RejectionObserver.class, TestKeyResolver.class))
            .overrideConfigKey(PREFIX + "paths", "/api/*")
            .overrideConfigKey(PREFIX + "phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "request-key", "custom:test-key")
            .overrideRuntimeConfigKey(PREFIX + "permits", "1")
            .overrideRuntimeConfigKey(PREFIX + "window", "1m");

    @Inject
    Vertx vertx;

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
    void equalLongKeysShareABucketAndDifferentOnesDoNot() {
        String first = "a".repeat(300);
        String second = "a".repeat(299) + "b";
        assertThat(http.get("/api/x", Map.of("X-Test-Key", first)).statusCode()).isEqualTo(200);
        assertThat(http.get("/api/x", Map.of("X-Test-Key", first)).statusCode()).isEqualTo(429);
        assertThat(http.get("/api/x", Map.of("X-Test-Key", second)).statusCode()).isEqualTo(200);
        assertThat(observer.keys).singleElement().asString().matches("sha256:[0-9a-f]{64}");
    }

    @Singleton
    @Identifier("test-key")
    public static class TestKeyResolver implements RateLimitKeyResolver {

        @Override
        public Uni<Optional<String>> resolve(RateLimitContext context) {
            return Uni.createFrom().item(Optional.ofNullable(context.routingContext().request().getHeader("X-Test-Key")));
        }
    }

    @Singleton
    public static class RejectionObserver {

        final List<String> keys = new CopyOnWriteArrayList<>();

        void observe(@Observes RateLimitRejectionInfo rejection) {
            keys.add(rejection.key());
        }
    }
}
