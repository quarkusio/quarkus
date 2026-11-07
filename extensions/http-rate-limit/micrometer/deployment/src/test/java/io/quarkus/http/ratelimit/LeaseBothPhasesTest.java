package io.quarkus.http.ratelimit;

import static io.quarkus.http.ratelimit.MetricsScrape.assertScrape;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.net.URL;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.http.ratelimit.store.SwitchableTestStore;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;

/**
 * A path limited in both phases by two lease rules: an unauthenticated request resolves {@code ip:<addr>} in both
 * phases, and each rule holds its own chunk under its own policy id.
 */
public class LeaseBothPhasesTest {

    private static final String PRE = "quarkus.http.rate-limit.pre.";
    private static final String POST = "quarkus.http.rate-limit.post.";

    @RegisterExtension
    static final QuarkusExtensionTest config = TestHttp.withUsers(SwitchableStoreApp.create("local", MetricsScrape.class))
            .overrideConfigKey(PRE + "paths", "/*")
            .overrideConfigKey(PRE + "phase", "pre-authentication")
            .overrideConfigKey(PRE + "store", "switchable")
            .overrideRuntimeConfigKey(PRE + "lease-size", "4")
            .overrideConfigKey(PRE + "request-key", "ip")
            .overrideRuntimeConfigKey(PRE + "permits", "8")
            .overrideRuntimeConfigKey(PRE + "window", "1m")
            .overrideConfigKey(POST + "paths", "/*")
            .overrideConfigKey(POST + "phase", "post-authentication")
            .overrideConfigKey(POST + "store", "switchable")
            .overrideRuntimeConfigKey(POST + "lease-size", "4")
            .overrideConfigKey(POST + "request-key", "principal")
            .overrideRuntimeConfigKey(POST + "permits", "8")
            .overrideRuntimeConfigKey(POST + "window", "1m")
            .setForcedDependencies(
                    List.of(Dependency.of("io.quarkus", "quarkus-elytron-security-properties-file-deployment",
                            Version.getVersion())));

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

    @BeforeEach
    void reset() {
        SwitchableTestStore.reset();
    }

    @Test
    void eachRuleHoldsItsOwnChunk() {
        assertThat(http.get("/x").statusCode()).isEqualTo(200);
        assertThat(http.get("/x").statusCode()).isEqualTo(200);
        // two claims per request (one per rule), each sized 1 then 2; no policy-change reset
        assertThat(SwitchableTestStore.costs).containsExactly(1L, 1L, 2L, 2L);
        assertScrape(http)
                .hasMetricWithLabelsAndValue("http_rate_limit_keys", 1, entry("rule", "pre"),
                        entry("phase", "pre-authentication"))
                .hasMetricWithLabelsAndValue("http_rate_limit_keys", 1, entry("rule", "post"),
                        entry("phase", "post-authentication"))
                .hasMetricWithLabelsAndValue("http_rate_limit_lease_stranded_total", 0,
                        entry("phase", "pre-authentication"));
    }
}
