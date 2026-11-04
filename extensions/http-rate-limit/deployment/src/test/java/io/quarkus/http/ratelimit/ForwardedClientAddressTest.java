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

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;

/**
 * Behind a proxy, the client address comes from {@code X-Forwarded-For}, which may name a host rather than an IP
 * literal. The {@code ip} key and the address in the rejection must be read the same way, so that an observer sees the
 * address the limit was keyed on, and distinct forwarded clients must not share one bucket.
 */
public class ForwardedClientAddressTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = TestHttp.ipRule(new QuarkusExtensionTest(), "api", "/api/*", 1, "1m")
            .withApplicationRoot(
                    jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class, Observer.class))
            .overrideRuntimeConfigKey("quarkus.http.proxy.proxy-address-forwarding", "true")
            .overrideRuntimeConfigKey("quarkus.http.proxy.allow-x-forwarded", "true");

    @Inject
    Vertx vertx;

    @Inject
    Observer observer;

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
    void forwardedHostNamesAreSeparateClientsAndMatchTheRejection() {
        Map<String, String> a = Map.of("X-Forwarded-For", "a.client.example");
        Map<String, String> b = Map.of("X-Forwarded-For", "b.client.example");

        assertThat(http.get("/api/x", a).statusCode()).isEqualTo(200);
        assertThat(http.get("/api/x", a).statusCode()).isEqualTo(429);
        // another forwarded client has its own bucket
        assertThat(http.get("/api/x", b).statusCode()).isEqualTo(200);

        // the observer is shared by the test methods: look at this client's rejections only
        assertThat(observer.received).filteredOn(r -> r.key().endsWith(".client.example")).singleElement()
                .satisfies(rejection -> {
                    assertThat(rejection.remoteAddress()).isEqualTo("a.client.example");
                    assertThat(rejection.key()).isEqualTo("ip:" + rejection.remoteAddress());
                });
    }

    @Test
    void forwardedIpLiteralMatchesTheRejection() {
        Map<String, String> c = Map.of("X-Forwarded-For", "203.0.113.7");

        assertThat(http.get("/api/x", c).statusCode()).isEqualTo(200);
        assertThat(http.get("/api/x", c).statusCode()).isEqualTo(429);

        RateLimitRejectionInfo rejection = observer.received.stream()
                .filter(r -> "203.0.113.7".equals(r.remoteAddress())).findFirst().orElseThrow();
        assertThat(rejection.key()).isEqualTo("ip:203.0.113.7");
    }

    @Singleton
    public static class Observer {

        final List<RateLimitRejectionInfo> received = new CopyOnWriteArrayList<>();

        void observe(@Observes RateLimitRejectionInfo rejection) {
            received.add(rejection);
        }
    }
}
