package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

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

public class PrincipalWithoutMechanismWarningTest {

    private static final String PREFIX = "quarkus.http.rate-limit.rules.api.";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class,
                    Rejections.class))
            .overrideConfigKey("quarkus.vertx.event-loops-pool-size", "1")
            // no mechanism at all
            .overrideConfigKey("quarkus.http.auth.basic", "false")
            .overrideConfigKey(PREFIX + "paths", "/api/*")
            .overrideConfigKey(PREFIX + "phase", "post-authentication")
            .overrideConfigKey(PREFIX + "request-key", "principal")
            .overrideRuntimeConfigKey(PREFIX + "permits", "1")
            .overrideRuntimeConfigKey(PREFIX + "window", "1m")
            .setLogRecordPredicate(r -> r.getLoggerName().startsWith("io.quarkus.http.ratelimit"))
            .assertLogRecords(records -> assertThat(records).anySatisfy(r -> {
                assertThat(r.getLevel()).isEqualTo(Level.WARNING);
                assertThat(r.getMessage()).isEqualTo("Rate limit rule 'api' uses request-key 'principal' but no "
                        + "HttpAuthenticationMechanism bean is present; every request will be anonymous");
            }));

    @Inject
    Vertx vertx;

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
    void everyRequestIsAnonymous() {
        // limited by address, the fallback of the principal key
        assertThat(http.get("/api/x").statusCode()).isEqualTo(200);
        assertThat(http.get("/api/x").statusCode()).isEqualTo(429);
        assertThat(rejections.principals).containsExactly(Optional.empty());
    }

    @Singleton
    public static class Rejections {

        final List<Optional<String>> principals = new CopyOnWriteArrayList<>();

        void onRejection(@Observes RateLimitRejectionInfo rejection) {
            principals.add(rejection.principal());
        }
    }
}
