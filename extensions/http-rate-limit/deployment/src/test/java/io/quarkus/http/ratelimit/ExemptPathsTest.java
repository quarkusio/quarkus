package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.http.PoolOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

/**
 * Health probes and the metrics endpoint must never be rate limited, while other paths,
 * including other non-application paths under /q, are.
 */
public class ExemptPathsTest {

    private static final List<String> EXEMPT_PATHS = List.of(
            "/q/health/live", "/q/health/ready", "/q/health/started", "/q/metrics");

    @RegisterExtension
    static final QuarkusExtensionTest config = TestHttp.ipRule(new QuarkusExtensionTest(), "all", "/*", 1, "1m")
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestHttp.Resource.class,
                    TestRateLimitClock.class, RouterInit.class))
            .overrideConfigKey("quarkus.vertx.event-loops-pool-size", "1")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-smallrye-health-deployment", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-micrometer-registry-prometheus-deployment",
                            Version.getVersion())));

    @Inject
    Vertx vertx;

    @TestHTTPResource
    URL url;

    @Test
    void exemptPathsAreNeverLimited() {
        WebClient client = WebClient.create(vertx, new WebClientOptions(), new PoolOptions().setHttp1MaxSize(1));
        try {
            assertThat(TestHttp.get(vertx, client, url, "/answer", Map.of()).statusCode()).isEqualTo(200);
            for (String path : EXEMPT_PATHS) {
                assertThat(TestHttp.get(vertx, client, url, path, Map.of()).statusCode()).as(path).isEqualTo(200);
            }
            assertThat(TestHttp.get(vertx, client, url, "/answer", Map.of()).statusCode()).isEqualTo(429);
            assertThat(TestHttp.get(vertx, client, url, "/q/answer", Map.of()).statusCode()).isEqualTo(429);
        } finally {
            client.close();
        }
    }

    @Singleton
    public static class RouterInit {
        public void init(@Observes Router router) {
            router.route("/q/answer").handler(ctx -> ctx.response().end());
        }
    }
}
