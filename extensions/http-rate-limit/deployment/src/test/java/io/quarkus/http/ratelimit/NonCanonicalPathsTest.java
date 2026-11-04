package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;

/**
 * A request path can name the same resource in many forms: the REST layer ignores matrix parameters ({@code ;name=value})
 * in a path segment, so {@code /login;x=1} reaches the resource of {@code /login}, and paths may be percent-encoded, even
 * several times, or contain dot segments, backslashes or null bytes. The limiter must match every such form like the
 * canonical path, or rewriting the path would get around a limit. The rejection event reports the canonical path too.
 */
public class NonCanonicalPathsTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = TestHttp.ipRule(
            TestHttp.ipRule(new QuarkusExtensionTest(), "login", "/login", 1, "1h"), "api", "/api/*", 1, "1h")
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, Login.class, Api.class,
                    PerPath.class, RejectionObserver.class))
            .overrideConfigKey("quarkus.http.rate-limit.per-path.paths", "/per-path/*")
            .overrideConfigKey("quarkus.http.rate-limit.per-path.phase", "pre-authentication")
            .overrideConfigKey("quarkus.http.rate-limit.per-path.request-key", "path")
            .overrideRuntimeConfigKey("quarkus.http.rate-limit.per-path.permits", "1")
            .overrideRuntimeConfigKey("quarkus.http.rate-limit.per-path.window", "1h");

    @Inject
    Vertx vertx;

    @TestHTTPResource
    URL url;

    @Inject
    RejectionObserver observer;

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
    void nonCanonicalFormsOfAnExactPathShareItsLimit() {
        assertThat(http.get("/login").statusCode()).isEqualTo(200);
        assertLimited("/login;x=1", "/login;",
                // percent-encoded once, twice and three times
                "/%6Cogin", "/%256Cogin", "/%25256Cogin",
                // encoded semicolons, encoded and double-encoded dot segments, backslashes, null bytes
                "/login%3Bx=1", "/login%253Bx=1", "/x/%2e%2e/login", "/x/%252e%252e/login", "/x/..%2Flogin",
                "/x%5C..%5Clogin", "/login%00");
        assertThat(observer.pathsOf("login")).hasSize(12).containsOnly("/login");
    }

    @Test
    void nonCanonicalFormsOfAPrefixShareItsLimit() {
        assertThat(http.get("/api/a").statusCode()).isEqualTo(200);
        assertLimited("/api;x=1/a", "/api/a;x=1", "/api%3Bx/a", "/%61pi/a", "/%2561pi/a", "/x/%2e%2e/api/a",
                "/api/%2e/a", "/x%5C..%5Capi/a", "/api%00/a");
    }

    @Test
    void nonCanonicalFormsOfAPathShareItsPathKey() {
        assertThat(http.get("/per-path/a").statusCode()).isEqualTo(200);
        assertLimited("/per-path/a;x=1", "/per-path/%61", "/per-path/%2561", "/per-path/x/%2e%2e/a");
        // another path has a bucket of its own
        assertThat(http.get("/per-path/b").statusCode()).isEqualTo(200);
    }

    private void assertLimited(String... paths) {
        for (String path : paths) {
            assertThat(http.get(path).statusCode()).as(path).isEqualTo(429);
        }
    }

    @Singleton
    public static class RejectionObserver {

        final List<RateLimitRejectionInfo> rejections = new CopyOnWriteArrayList<>();

        void observe(@Observes RateLimitRejectionInfo rejection) {
            rejections.add(rejection);
        }

        List<String> pathsOf(String rule) {
            return rejections.stream().filter(r -> r.rule().equals(rule)).map(RateLimitRejectionInfo::path).toList();
        }
    }

    @Path("/login")
    public static class Login {

        @GET
        public String get() {
            return "login";
        }
    }

    @Path("/per-path")
    public static class PerPath {

        @GET
        @Path("{p: .*}")
        public String get() {
            return "per-path";
        }
    }

    @Path("/api")
    public static class Api {

        @GET
        @Path("{p: .*}")
        public String get() {
            return "api";
        }
    }
}
