package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;

/**
 * The built-in {@code principal} key falls back to the client address for a request without a non-anonymous identity;
 * a custom post-authentication resolver sees such requests too and decides itself.
 */
public class AnonymousFallbackTest {

    private static final String PREFIX = "quarkus.http.rate-limit.";

    @RegisterExtension
    static final QuarkusExtensionTest config = withRule(withRule(withRule(
            TestHttp.withUsers(new QuarkusExtensionTest()
                    .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class,
                            RecordingResolver.class, Rejections.class)))
                    .overrideConfigKey("quarkus.vertx.event-loops-pool-size", "1")
                    .overrideConfigKey("quarkus.http.auth.permission.protected.paths", "/protected/*")
                    .overrideConfigKey("quarkus.http.auth.permission.protected.policy", "authenticated"),
            "principal", "principal"), "protected", "principal"), "custom", "custom:recording")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-elytron-security-properties-file-deployment", Version.getVersion())));

    private static QuarkusExtensionTest withRule(QuarkusExtensionTest test, String rule, String key) {
        return test.overrideConfigKey(PREFIX + "rules." + rule + ".paths", "/" + rule + "/*")
                .overrideConfigKey(PREFIX + "rules." + rule + ".phase", "post-authentication")
                .overrideConfigKey(PREFIX + "rules." + rule + ".request-key", key)
                .overrideRuntimeConfigKey(PREFIX + "rules." + rule + ".permits", "1")
                .overrideRuntimeConfigKey(PREFIX + "rules." + rule + ".window", "1m");
    }

    @Inject
    Vertx vertx;

    @Inject
    @Identifier("recording")
    RecordingResolver resolver;

    @Inject
    Rejections rejections;

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
    void anonymousRequestIsLimitedPerAddressAndAuthenticatedPerPrincipal() {
        assertThat(get("/principal/x", Map.of()).statusCode()).isEqualTo(200);
        assertThat(get("/principal/x", Map.of()).statusCode()).isEqualTo(429);
        assertThat(rejections.keys("principal")).singleElement().asString().startsWith("ip:");
        // the principal has its own bucket, separate from the address it connects from
        assertThat(get("/principal/x", TestHttp.basic("alice", "alice")).statusCode()).isEqualTo(200);
        assertThat(get("/principal/x", TestHttp.basic("alice", "alice")).statusCode()).isEqualTo(429);
        assertThat(rejections.keys("principal")).last().isEqualTo("principal:alice");
    }

    @Test
    void anonymousRequestOnProtectedPathIsCountedBeforeAuthorization() {
        // the limiter runs before the permission check: the first anonymous request is let through to it and gets 401
        assertThat(get("/protected/x", Map.of()).statusCode()).isEqualTo(401);
        assertThat(get("/protected/x", Map.of()).statusCode()).isEqualTo(429);
    }

    @Test
    void wrongCredentialsNeverReachThePostAuthenticationLimiter() {
        for (int i = 0; i < 3; i++) {
            assertThat(get("/protected/x", TestHttp.basic("bob", "wrong")).statusCode()).isEqualTo(401);
        }
        assertThat(rejections.keys("protected")).allSatisfy(key -> assertThat(key).doesNotContain("bob"));
    }

    @Test
    void customResolverReceivesAnonymousRequest() {
        HttpResponse<Buffer> response = get("/custom/x", Map.of());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(resolver.anonymousCalls).hasPositiveValue();
    }

    private HttpResponse<Buffer> get(String path, Map<String, String> headers) {
        return http.get(path, headers);
    }

    /**
     * Counts the anonymous requests it is called for, and skips every request.
     */
    @Singleton
    @Identifier("recording")
    public static class RecordingResolver implements RateLimitKeyResolver {

        final AtomicInteger anonymousCalls = new AtomicInteger();

        @Override
        public Uni<Optional<String>> resolve(RateLimitContext context) {
            if (context.identity().map(SecurityIdentity::isAnonymous).orElse(true)) {
                anonymousCalls.incrementAndGet();
            }
            return Uni.createFrom().item(Optional.empty());
        }
    }

    @Singleton
    public static class Rejections {

        private final List<RateLimitRejectionInfo> rejections = new CopyOnWriteArrayList<>();

        void onRejection(@Observes RateLimitRejectionInfo rejection) {
            rejections.add(rejection);
        }

        List<String> keys(String rule) {
            return rejections.stream().filter(r -> r.rule().equals(rule)).map(info -> info.key().orElseThrow()).toList();
        }
    }
}
