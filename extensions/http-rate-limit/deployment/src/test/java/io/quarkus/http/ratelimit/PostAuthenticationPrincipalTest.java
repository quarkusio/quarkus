package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

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
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.client.HttpResponse;

/**
 * {@code api} and {@code probe} are separate rules, so each test has its own buckets for the same principal.
 */
public class PostAuthenticationPrincipalTest {

    private static final String PREFIX = "quarkus.http.rate-limit.";

    @RegisterExtension
    static final QuarkusExtensionTest config = withRule(withRule(TestHttp.withUsers(new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class)))
            .overrideConfigKey("quarkus.vertx.event-loops-pool-size", "1")
            .overrideRuntimeConfigKey("quarkus.http.auth.policy.admin-policy.roles-allowed", "admin")
            .overrideConfigKey("quarkus.http.auth.permission.admin.paths", "/probe/*")
            .overrideRuntimeConfigKey("quarkus.http.auth.permission.admin.policy", "admin-policy"),
            "api", "/api/*"), "probe", "/probe/*")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-elytron-security-properties-file-deployment", Version.getVersion())));

    private static QuarkusExtensionTest withRule(QuarkusExtensionTest test, String rule, String path) {
        return test.overrideConfigKey(PREFIX + rule + ".paths", path)
                .overrideConfigKey(PREFIX + rule + ".phase", "post-authentication")
                .overrideConfigKey(PREFIX + rule + ".request-key", "principal")
                .overrideRuntimeConfigKey(PREFIX + rule + ".permits", "2")
                .overrideRuntimeConfigKey(PREFIX + rule + ".window", "1m");
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
    void limitsPerPrincipal() {
        Map<String, String> alice = TestHttp.basic("alice", "alice");
        assertThat(get("/api/x", alice).statusCode()).isEqualTo(200);
        assertThat(get("/api/x", alice).statusCode()).isEqualTo(200);
        HttpResponse<Buffer> rejected = get("/api/x", alice);
        assertThat(rejected.statusCode()).isEqualTo(429);
        assertThat(Long.parseLong(rejected.getHeader(HttpHeaders.RETRY_AFTER.toString()))).isBetween(1L, 30L);

        assertThat(get("/api/x", TestHttp.basic("bob", "bob")).statusCode()).isEqualTo(200);
    }

    @Test
    void probingForbiddenPathStillSpendsQuota() {
        Map<String, String> alice = TestHttp.basic("alice", "alice");
        // the limiter runs before authorization, so a forbidden request spends a permit too
        assertThat(get("/probe/x", alice).statusCode()).isEqualTo(403);
        assertThat(get("/probe/x", alice).statusCode()).isEqualTo(403);
        assertThat(get("/probe/x", alice).statusCode()).isEqualTo(429);
    }

    private HttpResponse<Buffer> get(String path, Map<String, String> headers) {
        return http.get(path, headers);
    }
}
