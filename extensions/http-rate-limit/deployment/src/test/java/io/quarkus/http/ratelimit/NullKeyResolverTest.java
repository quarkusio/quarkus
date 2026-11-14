package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
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

/**
 * A resolver that emits {@code null} instead of an {@link Optional} is handled like one that fails: the request is
 * keyed by its client address and a warning is logged, rather than the request failing.
 */
public class NullKeyResolverTest {

    private static final String RULE = "quarkus.http.rate-limit.rules.lenient.";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideRuntimeConfigKey("quarkus.http.rate-limit.key-resolver-failure", "ip")
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class,
                    NullResolver.class))
            .overrideConfigKey(RULE + "paths", "/lenient/*")
            .overrideConfigKey(RULE + "phase", "pre-authentication")
            .overrideConfigKey(RULE + "request-key", "custom:null")
            .overrideRuntimeConfigKey(RULE + "permits", "1")
            .overrideRuntimeConfigKey(RULE + "window", "1m")
            .setLogRecordPredicate(r -> r.getLoggerName().startsWith("io.quarkus.http.ratelimit"))
            .assertLogRecords(records -> assertThat(records)
                    .filteredOn(r -> r.getMessage().contains("key resolution failed"))
                    .singleElement()
                    .satisfies(r -> assertThat(String.format(r.getMessage(), r.getParameters()))
                            .isEqualTo("Rate limit rule 'lenient' key resolution failed; the request is keyed by its"
                                    + " client address")));

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
    void nullKeyFallsBackToTheClientAddress() {
        assertThat(http.get("/lenient/x").statusCode()).isEqualTo(200);
        assertThat(http.get("/lenient/x").statusCode()).isEqualTo(429);
    }

    @Singleton
    @Identifier("null")
    public static class NullResolver implements RateLimitKeyResolver {

        @Override
        public Uni<Optional<String>> resolve(RateLimitContext context) {
            return Uni.createFrom().nullItem();
        }
    }
}
