package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

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
import io.vertx.ext.web.client.HttpResponse;

/**
 * The router keeps an encoded slash or backslash inside its path segment, while the canonical path decodes it and then
 * resolves the dot segments. {@code /api/items/..%2F..%2Fpublic} therefore reaches {@code /api/items/{id}}, although
 * its canonical path is {@code /public}. The limiter must apply the rules of both paths, and must not exempt a request
 * whose canonical path alone is exempt, or a client could steer any request out of its rule.
 */
public class PathSteeringTest {

    private static final String PREFIX = "quarkus.http.rate-limit.rules.api.";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, Items.class))
            .overrideConfigKey(PREFIX + "paths", "/api/*")
            .overrideConfigKey(PREFIX + "phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "request-key", "header:X-Client")
            .overrideRuntimeConfigKey(PREFIX + "permits", "1")
            .overrideRuntimeConfigKey(PREFIX + "window", "1h")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-smallrye-health-deployment", Version.getVersion())));

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
    void steeringToAPathWithoutARuleIsLimited() {
        assertSteeredAndLimited("/api/items/..%2F..%2Fpublic");
        assertSteeredAndLimited("/api/items/..%252F..%252Fpublic");
        assertSteeredAndLimited("/api/items/..%5C..%5Cpublic");
    }

    @Test
    void steeringToAnExemptPathIsLimited() {
        assertSteeredAndLimited("/api/items/..%2F..%2F..%2Fq%2Fhealth%2Flive");
    }

    @Test
    void exemptPathsStayExempt() {
        String client = UUID.randomUUID().toString();
        for (int i = 0; i < 3; i++) {
            assertThat(http.get("/q/health/live", Map.of("X-Client", client)).statusCode()).isEqualTo(200);
        }
    }

    /**
     * The first request of a fresh client shows where the router sends the path; the second one must be refused.
     */
    private void assertSteeredAndLimited(String path) {
        Map<String, String> client = Map.of("X-Client", UUID.randomUUID().toString());
        HttpResponse<Buffer> first = http.get(path, client);
        assertThat(first.statusCode()).as(path).isEqualTo(200);
        assertThat(first.bodyAsString()).as(path).startsWith("item:");
        assertThat(http.get(path, client).statusCode()).as(path).isEqualTo(429);
    }

    @Path("/api/items")
    public static class Items {

        @GET
        @Path("{id}")
        public String get(@PathParam("id") String id) {
            return "item:" + id;
        }
    }
}
