package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;

public class AsyncObserverPostAuthenticationTest {

    private static final String PREFIX = "quarkus.http.rate-limit.api.";

    @RegisterExtension
    static final QuarkusExtensionTest config = TestHttp.withUsers(new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class,
                    TestRateLimitClock.class, AsyncObserver.class)))
            .overrideConfigKey("quarkus.vertx.event-loops-pool-size", "1")
            .overrideConfigKey(PREFIX + "paths", "/api/*")
            .overrideConfigKey(PREFIX + "phase", "post-authentication")
            .overrideConfigKey(PREFIX + "request-key", "principal")
            .overrideRuntimeConfigKey(PREFIX + "permits", "1")
            .overrideRuntimeConfigKey(PREFIX + "window", "1m")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-elytron-security-properties-file-deployment", Version.getVersion())));

    @Inject
    Vertx vertx;

    @Inject
    AsyncObserver observer;

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
    void snapshotCarriesThePrincipal() {
        Map<String, String> alice = TestHttp.basic("alice", "alice");
        assertThat(http.get("/api/x", alice).statusCode()).isEqualTo(200);
        assertThat(http.get("/api/x", alice).statusCode()).isEqualTo(429);

        await().atMost(Duration.ofSeconds(5)).until(() -> !observer.received.isEmpty());
        RateLimitRejectionInfo rejection = observer.received.getFirst();
        assertThat(rejection.phase()).isEqualTo(RateLimitPhase.POST_AUTHENTICATION);
        assertThat(rejection.method()).isEqualTo("GET");
        assertThat(rejection.path()).isEqualTo("/api/x");
        assertThat(rejection.remoteAddress()).isEqualTo("127.0.0.1");
        assertThat(rejection.principal()).hasValue("alice");
    }

    @Singleton
    public static class AsyncObserver {

        final List<RateLimitRejectionInfo> received = new CopyOnWriteArrayList<>();

        void observe(@ObservesAsync RateLimitRejectionInfo rejection) {
            received.add(rejection);
        }
    }
}
