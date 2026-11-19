package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
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
import io.vertx.core.http.HttpMethod;

public class AsyncObserverTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = TestHttp.ipRule(new QuarkusExtensionTest(), "api", "/api/*", 1, "1m")
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class,
                    TestRateLimitClock.class, AsyncObserver.class, ThrowingAsyncObserver.class, SyncObserver.class))
            .overrideConfigKey("quarkus.vertx.event-loops-pool-size", "1");

    @Inject
    Vertx vertx;

    @Inject
    AsyncObserver observer;

    @Inject
    SyncObserver syncObserver;

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
    void asyncAndSyncObserversAreNotified() {
        assertThat(http.get("/api/x").statusCode()).isEqualTo(200);
        // the throwing async observer does not change the response
        assertThat(http.send(HttpMethod.POST, "/api/a/../x", Map.of(), Buffer.buffer("b"))
                .statusCode()).isEqualTo(429);

        await().atMost(Duration.ofSeconds(5)).until(() -> !observer.received.isEmpty());
        RateLimitRejectionInfo rejection = observer.received.getFirst();
        assertThat(rejection.rule()).isEqualTo("api");
        // the snapshot of the request, read after the request has ended
        assertThat(rejection.method()).isEqualTo("POST");
        assertThat(rejection.path()).isEqualTo("/api/x");
        assertThat(rejection.remoteAddress()).isEqualTo("127.0.0.1");
        // before authentication, no identity is known
        assertThat(rejection.principal()).isEmpty();
        assertThat(syncObserver.received).hasSize(1);
        assertThat(syncObserver.received.getFirst().rule()).isEqualTo("api");
    }

    @Singleton
    public static class AsyncObserver {

        final List<RateLimitRejectionInfo> received = new CopyOnWriteArrayList<>();

        void observe(@ObservesAsync RateLimitRejectionInfo rejection) {
            received.add(rejection);
        }
    }

    @Singleton
    public static class ThrowingAsyncObserver {

        void observe(@ObservesAsync RateLimitRejectionInfo rejection) {
            throw new IllegalStateException("async observer failure");
        }
    }

    @Singleton
    public static class SyncObserver {

        final List<RateLimitRejectionInfo> received = new CopyOnWriteArrayList<>();

        void observe(@Observes RateLimitRejectionInfo rejection) {
            received.add(rejection);
        }
    }
}
