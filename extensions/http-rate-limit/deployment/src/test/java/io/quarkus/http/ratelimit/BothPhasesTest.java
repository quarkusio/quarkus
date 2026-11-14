package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

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
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.client.HttpResponse;

/**
 * One path limited in both phases, by two rules: a request rejected after authentication has already spent its
 * pre-authentication permit.
 */
public class BothPhasesTest {

    private static final String PRE = "quarkus.http.rate-limit.rules.pre.";
    private static final String POST = "quarkus.http.rate-limit.rules.post.";

    @RegisterExtension
    static final QuarkusExtensionTest config = TestHttp.withUsers(new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class,
                    TestRateLimitClock.class, RejectionObserver.class)))
            .overrideConfigKey("quarkus.vertx.event-loops-pool-size", "1")
            .overrideConfigKey(PRE + "paths", "/api/*")
            .overrideConfigKey(PRE + "phase", "pre-authentication")
            .overrideConfigKey(PRE + "request-key", "ip")
            .overrideRuntimeConfigKey(PRE + "permits", "3")
            .overrideRuntimeConfigKey(PRE + "window", "1m")
            .overrideConfigKey(POST + "paths", "/api/*")
            .overrideConfigKey(POST + "phase", "post-authentication")
            .overrideConfigKey(POST + "request-key", "principal")
            .overrideRuntimeConfigKey(POST + "permits", "1")
            .overrideRuntimeConfigKey(POST + "window", "1m")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-elytron-security-properties-file-deployment", Version.getVersion())));

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
    void preRejectionNeverReachesPost() {
        Map<String, String> alice = TestHttp.basic("alice", "alice");
        assertThat(http.get("/api/x", alice).statusCode()).isEqualTo(200);

        // the post-authentication limit: one permit of 60 s
        HttpResponse<Buffer> second = http.get("/api/x", alice);
        assertThat(second.statusCode()).isEqualTo(429);
        assertThat(retryAfter(second)).isEqualTo(60);
        HttpResponse<Buffer> third = http.get("/api/x", alice);
        assertThat(third.statusCode()).isEqualTo(429);
        assertThat(retryAfter(third)).isEqualTo(60);

        // the pre-authentication limit: three permits of 20 s each, all spent by the requests above
        HttpResponse<Buffer> fourth = http.get("/api/x", alice);
        assertThat(fourth.statusCode()).isEqualTo(429);
        assertThat(retryAfter(fourth)).isEqualTo(20);
        assertThat(observer.phases).containsExactly(RateLimitPhase.POST_AUTHENTICATION,
                RateLimitPhase.POST_AUTHENTICATION, RateLimitPhase.PRE_AUTHENTICATION);
    }

    private static long retryAfter(HttpResponse<Buffer> response) {
        return Long.parseLong(response.getHeader(HttpHeaders.RETRY_AFTER.toString()));
    }

    @Singleton
    public static class RejectionObserver {

        final List<RateLimitPhase> phases = new CopyOnWriteArrayList<>();

        void observe(@Observes RateLimitRejectionInfo rejection) {
            phases.add(rejection.phase());
        }
    }
}
