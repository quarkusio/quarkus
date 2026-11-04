package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.Map;
import java.util.Optional;

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

public class CustomKeyResolverTest {

    private static final String PREFIX = "quarkus.http.rate-limit.api.";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class,
                    TestRateLimitClock.class, TenantResolver.class))
            .overrideConfigKey("quarkus.vertx.event-loops-pool-size", "1")
            .overrideConfigKey(PREFIX + "paths", "/*")
            .overrideConfigKey(PREFIX + "phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "request-key", "custom:tenant")
            .overrideRuntimeConfigKey(PREFIX + "permits", "1")
            .overrideRuntimeConfigKey(PREFIX + "window", "1m");

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
    void customResolverSelectedByIdentifier() {
        assertThat(get(Map.of("X-Tenant", "a"))).isEqualTo(200);
        assertThat(get(Map.of("X-Tenant", "a"))).isEqualTo(429);
        assertThat(get(Map.of("X-Tenant", "b"))).isEqualTo(200);
        // no header: the resolver returns empty, the rule does not apply
        for (int i = 0; i < 3; i++) {
            assertThat(get(Map.of())).isEqualTo(200);
        }
    }

    private int get(Map<String, String> headers) {
        return http.get("/x", headers).statusCode();
    }

    @Singleton
    @Identifier("tenant")
    public static class TenantResolver implements RateLimitKeyResolver {

        @Override
        public Uni<Optional<String>> resolve(RateLimitContext context) {
            return Uni.createFrom().item(
                    Optional.ofNullable(context.routingContext().request().getHeader("X-Tenant")));
        }
    }
}
