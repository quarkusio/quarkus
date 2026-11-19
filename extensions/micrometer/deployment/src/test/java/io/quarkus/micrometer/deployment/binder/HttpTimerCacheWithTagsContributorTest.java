package io.quarkus.micrometer.deployment.binder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.quarkus.micrometer.runtime.HttpServerMetricsTagsContributor;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;

/**
 * The HTTP server timers are cached: check the contributed tags are part of the cache key, and override the standard
 * tags, like without a cache.
 */
public class HttpTimerCacheWithTagsContributorTest {

    private static final String SERVER = "http.server.requests";

    @RegisterExtension
    static final QuarkusExtensionTest TEST = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(Resource.class, TenantContributor.class))
            .overrideConfigKey("quarkus.otel.enabled", "false")
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .overrideConfigKey("quarkus.micrometer.binder-enabled-default", "false")
            .overrideConfigKey("quarkus.micrometer.binder.http-server.enabled", "true")
            .overrideConfigKey("quarkus.micrometer.binder.vertx.enabled", "true");

    @Inject
    MeterRegistry registry;

    @Test
    public void contributedTagsAreRecorded() {
        RestAssured.given().header("tenant", "x").get("/contrib/a").then().statusCode(200);
        RestAssured.given().header("tenant", "x").get("/contrib/b").then().statusCode(200);
        RestAssured.given().header("tenant", "x").get("/contrib/a").then().statusCode(200);
        RestAssured.given().header("tenant", "y").get("/contrib/a").then().statusCode(200);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(count("x")).isEqualTo(3);
            assertThat(count("y")).isEqualTo(1);
        });
        // the contributed uri replaced the standard one
        assertThat(registry.find(SERVER).tag("uri", "/contrib/a").timers()).isEmpty();
        assertThat(registry.find(SERVER).tag("uri", "/contrib/b").timers()).isEmpty();
        assertThat(registry.find(SERVER).tag("uri", "/contrib/all").timers()).hasSize(2);
    }

    private long count(String tenant) {
        Timer timer = registry.find(SERVER).tag("uri", "/contrib/all").tag("tenant", tenant).timer();
        return timer == null ? 0 : timer.count();
    }

    @Singleton
    public static class TenantContributor implements HttpServerMetricsTagsContributor {
        @Override
        public Tags contribute(Context context) {
            String tenant = context.request().getHeader("tenant");
            return Tags.of("tenant", tenant == null ? "none" : tenant, "uri", "/contrib/all");
        }
    }

    @Path("/contrib")
    @Singleton
    public static class Resource {

        @GET
        @Path("/a")
        public String a() {
            return "a";
        }

        @GET
        @Path("/b")
        public String b() {
            return "b";
        }
    }
}
