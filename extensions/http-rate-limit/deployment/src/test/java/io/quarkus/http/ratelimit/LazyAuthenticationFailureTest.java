package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import jakarta.enterprise.event.Observes;
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
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;

/**
 * Under lazy authentication the deferred identity hands a failure to the authentication failure handler itself; the
 * limiter must neither fail the request a second time nor shape the response. {@code /api} is served by REST,
 * {@code /route} by a plain Vert.x route.
 */
public class LazyAuthenticationFailureTest {

    private static final String PREFIX = "quarkus.http.rate-limit.rules.api.";

    @RegisterExtension
    static final QuarkusExtensionTest config = TestHttp.withUsers(new QuarkusExtensionTest()
            .withApplicationRoot(
                    jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class, Routes.class)))
            .overrideConfigKey("quarkus.vertx.event-loops-pool-size", "1")
            .overrideConfigKey("quarkus.http.auth.proactive", "false")
            .overrideConfigKey(PREFIX + "paths", "/api/*,/route/*")
            .overrideConfigKey(PREFIX + "phase", "post-authentication")
            .overrideConfigKey(PREFIX + "request-key", "principal")
            .overrideRuntimeConfigKey(PREFIX + "permits", "100")
            .overrideRuntimeConfigKey(PREFIX + "window", "1m")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-elytron-security-properties-file-deployment", Version.getVersion())))
            .setLogRecordPredicate(r -> r.getLoggerName().contains("QuarkusErrorHandler")
                    || (r.getLoggerName().startsWith("io.vertx") && r.getLevel().intValue() >= Level.SEVERE.intValue()))
            .assertLogRecords(records -> assertThat(records).isEmpty());

    @Inject
    Vertx vertx;

    @Inject
    Routes routes;

    @TestHTTPResource
    URL url;

    TestClient http;

    @BeforeEach
    void createClient() {
        http = new TestClient(vertx, url);
        routes.failures.set(0);
    }

    @AfterEach
    void closeClient() {
        http.close();
    }

    @Test
    void wrongPasswordWithRest() {
        assertSingleChallenge("/api/x", "ok");
    }

    @Test
    void wrongPasswordWithoutRest() {
        assertSingleChallenge("/route/x", "route");
    }

    private void assertSingleChallenge(String path, String body) {
        HttpResponse<Buffer> challenged = http.get(path, TestHttp.basic("alice", "wrong"));
        assertThat(challenged.statusCode()).isEqualTo(401);
        assertThat(challenged.getHeader("WWW-Authenticate")).isNotNull();

        // the event loop runs the tasks queued by the failed request before it serves this one
        HttpResponse<Buffer> served = http.get(path, TestHttp.basic("alice", "alice"));
        assertThat(served.statusCode()).isEqualTo(200);
        assertThat(served.bodyAsString()).isEqualTo(body);
        assertThat(routes.failures.get()).isLessThanOrEqualTo(1);
    }

    @Singleton
    public static class Routes {

        final AtomicInteger failures = new AtomicInteger();

        void init(@Observes Router router) {
            router.route().order(Integer.MIN_VALUE).failureHandler(ctx -> {
                failures.incrementAndGet();
                ctx.next();
            });
            router.get("/route/*").handler(ctx -> ctx.end("route"));
        }
    }
}
