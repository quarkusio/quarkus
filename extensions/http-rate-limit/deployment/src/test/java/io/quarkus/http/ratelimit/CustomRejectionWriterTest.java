package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;

public class CustomRejectionWriterTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = TestHttp.ipRule(new QuarkusExtensionTest(), "api", "/*", 1, "1m")
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class,
                    TestRateLimitClock.class, CustomWriter.class))
            .overrideConfigKey("quarkus.vertx.event-loops-pool-size", "1");

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
    void customWriterAnswersTheRejection() {
        assertThat(http.get("/x").statusCode()).isEqualTo(200);
        HttpResponse<Buffer> rejected = http.get("/x");
        assertThat(rejected.statusCode()).isEqualTo(429);
        assertThat(rejected.bodyAsString()).isEqualTo("slow down");
        assertThat(rejected.getHeader("Content-Type")).startsWith("text/plain");
        assertThat(rejected.getHeader("X-Rule")).isEqualTo("api");
        // the writer does not set it: the handler did, before calling the writer
        assertThat(Long.parseLong(rejected.getHeader("Retry-After"))).isBetween(1L, 60L);
    }

    @Singleton
    @Alternative
    @Priority(1)
    public static class CustomWriter implements RateLimitRejectionWriter {

        @Override
        public void write(RoutingContext context, RateLimitRejectionInfo rejection) {
            context.response().setStatusCode(429)
                    .putHeader("Content-Type", "text/plain")
                    .putHeader("X-Rule", rejection.rule())
                    .end("slow down");
        }
    }
}
