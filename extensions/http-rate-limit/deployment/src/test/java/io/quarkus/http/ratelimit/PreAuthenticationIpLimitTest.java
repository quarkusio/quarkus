package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.PoolOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public class PreAuthenticationIpLimitTest {

    private static final String PREFIX = "quarkus.http.rate-limit.";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestHttp.Resource.class,
                    TestRateLimitClock.class, FailingResolver.class, ConnectionCounter.class))
            .overrideConfigKey("quarkus.vertx.event-loops-pool-size", "1")
            .overrideRuntimeConfigKey("quarkus.http.cors.enabled", "true")
            .overrideRuntimeConfigKey("quarkus.http.cors.origins", "https://app.example")
            // the only ip-keyed rule: every test shares the loopback address
            .overrideConfigKey(PREFIX + "api.paths", "/api/*")
            .overrideConfigKey(PREFIX + "api.phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "api.request-key", "ip")
            .overrideRuntimeConfigKey(PREFIX + "api.permits", "5")
            .overrideRuntimeConfigKey(PREFIX + "api.window", "1m")
            // the other rules are keyed by a header, with a unique value per test method
            .overrideConfigKey(PREFIX + "client.paths", "/client/*")
            .overrideConfigKey(PREFIX + "client.phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "client.request-key", "header:X-Client")
            .overrideRuntimeConfigKey(PREFIX + "client.permits", "1")
            .overrideRuntimeConfigKey(PREFIX + "client.window", "1m")
            .overrideConfigKey(PREFIX + "failing.paths", "/failing/*")
            .overrideConfigKey(PREFIX + "failing.phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "failing.request-key", "custom:failing")
            .overrideRuntimeConfigKey(PREFIX + "failing.permits", "1")
            .overrideRuntimeConfigKey(PREFIX + "failing.window", "1m");

    @Inject
    Vertx vertx;

    @Inject
    ConnectionCounter connections;

    @TestHTTPResource
    URL url;

    WebClient client;

    @BeforeEach
    void createClient() {
        client = WebClient.create(vertx, new WebClientOptions(), new PoolOptions().setHttp1MaxSize(1));
    }

    @AfterEach
    void closeClient() {
        client.close();
    }

    @Test
    void sixthRequestIsRejectedWith429AndRetryAfter() {
        for (int i = 0; i < 5; i++) {
            assertThat(get("/api/hello", Map.of()).statusCode()).isEqualTo(200);
        }
        HttpResponse<Buffer> rejected = get("/api/hello", Map.of());
        assertThat(rejected.statusCode()).isEqualTo(429);
        assertThat(Long.parseLong(rejected.getHeader(HttpHeaders.RETRY_AFTER.toString()))).isBetween(1L, 12L);
        assertThat(rejected.getHeader(HttpHeaders.CONTENT_TYPE.toString())).isEqualTo("text/plain; charset=utf-8");
        assertThat(rejected.bodyAsString()).isEqualTo("Too Many Requests");
    }

    @Test
    void rejectedHeadHasNoBody() {
        Map<String, String> headers = Map.of("X-Client", "head");
        assertThat(get("/client/x", headers).statusCode()).isEqualTo(200);
        HttpResponse<Buffer> rejected = send(HttpMethod.HEAD, "/client/x", headers, null);
        assertThat(rejected.statusCode()).isEqualTo(429);
        assertThat(rejected.getHeader(HttpHeaders.RETRY_AFTER.toString())).isNotNull();
        assertThat(rejected.body() == null ? 0 : rejected.body().length()).isZero();
    }

    @Test
    void throwingResolverKeysByTheClientAddress() {
        assertThat(get("/failing/x", Map.of()).statusCode()).isEqualTo(200);
        for (int i = 0; i < 10; i++) {
            assertThat(get("/failing/x", Map.of()).statusCode()).isEqualTo(429);
        }
    }

    @Test
    void rejectionDoesNotConsume() {
        Map<String, String> headers = Map.of("X-Client", "no-consume");
        assertThat(get("/client/x", headers).statusCode()).isEqualTo(200);
        HttpResponse<Buffer> first = get("/client/x", headers);
        HttpResponse<Buffer> second = get("/client/x", headers);
        assertThat(first.statusCode()).isEqualTo(429);
        assertThat(second.statusCode()).isEqualTo(429);
        long firstRetryAfter = Long.parseLong(first.getHeader(HttpHeaders.RETRY_AFTER.toString()));
        long secondRetryAfter = Long.parseLong(second.getHeader(HttpHeaders.RETRY_AFTER.toString()));
        assertThat(secondRetryAfter).isBetween(firstRetryAfter - 1, firstRetryAfter + 1);
    }

    @Test
    void connectionSurvivesRejectedPostWithUnreadBody() {
        assertThat(send(HttpMethod.POST, "/client/upload", Map.of("X-Client", "exhausted"), Buffer.buffer("x"))
                .statusCode()).isEqualTo(200);
        HttpResponse<Buffer> rejected = send(HttpMethod.POST, "/client/upload", Map.of("X-Client", "exhausted"),
                Buffer.buffer(new byte[1024 * 1024]));
        assertThat(rejected.statusCode()).isEqualTo(429);
        assertThat(get("/client/upload", Map.of("X-Client", "fresh")).statusCode()).isEqualTo(200);
        assertThat(connections.count()).isEqualTo(1);
    }

    @Test
    void corsHeadersPresentOn429AndPreflightIsFree() {
        Map<String, String> preflight = Map.of("X-Client", "cors", "Origin", "https://app.example",
                "Access-Control-Request-Method", "GET");
        for (int i = 0; i < 10; i++) {
            HttpResponse<Buffer> response = send(HttpMethod.OPTIONS, "/client/cors", preflight, null);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("https://app.example");
        }
        Map<String, String> withOrigin = Map.of("X-Client", "cors", "Origin", "https://app.example");
        assertThat(get("/client/cors", withOrigin).statusCode()).isEqualTo(200);

        assertThat(send(HttpMethod.OPTIONS, "/client/cors", Map.of("X-Client", "cors"), null).statusCode())
                .isEqualTo(429);

        HttpResponse<Buffer> rejected = get("/client/cors", withOrigin);
        assertThat(rejected.statusCode()).isEqualTo(429);
        assertThat(rejected.getHeader("Access-Control-Allow-Origin")).isEqualTo("https://app.example");
    }

    private HttpResponse<Buffer> get(String path, Map<String, String> headers) {
        return TestHttp.get(vertx, client, url, path, headers);
    }

    private HttpResponse<Buffer> send(HttpMethod method, String path, Map<String, String> headers, Buffer body) {
        return TestHttp.send(vertx, client, url, method, path, headers, body);
    }

    @Singleton
    @Identifier("failing")
    public static class FailingResolver implements RateLimitKeyResolver {

        @Override
        public Uni<Optional<String>> resolve(RateLimitContext context) {
            throw new IllegalStateException("resolver failure");
        }
    }

    /**
     * Counts the distinct connections that carried requests to {@code /client/upload}.
     */
    @Singleton
    public static class ConnectionCounter {

        private final Set<HttpConnection> connections = Collections.synchronizedSet(
                Collections.newSetFromMap(new IdentityHashMap<>()));

        void init(@Observes Router router) {
            router.route("/client/upload").order(Integer.MIN_VALUE + 2).handler(ctx -> {
                connections.add(ctx.request().connection());
                ctx.next();
            });
        }

        int count() {
            return connections.size();
        }
    }
}
