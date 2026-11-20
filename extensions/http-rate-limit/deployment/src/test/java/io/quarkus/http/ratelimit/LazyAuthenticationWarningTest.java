package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.LogRecord;

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

public class LazyAuthenticationWarningTest {

    private static final String PREFIX = "quarkus.http.rate-limit.rules.api.";

    @RegisterExtension
    static final QuarkusExtensionTest config = TestHttp.withUsers(new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class,
                    Rejections.class)))
            .overrideConfigKey("quarkus.vertx.event-loops-pool-size", "1")
            .overrideConfigKey("quarkus.http.auth.proactive", "false")
            .overrideConfigKey(PREFIX + "paths", "/api/*")
            .overrideConfigKey(PREFIX + "phase", "post-authentication")
            .overrideConfigKey(PREFIX + "request-key", "principal")
            .overrideRuntimeConfigKey(PREFIX + "permits", "1")
            .overrideRuntimeConfigKey(PREFIX + "window", "1m")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-elytron-security-properties-file-deployment", Version.getVersion())))
            .setLogRecordPredicate(r -> r.getLoggerName().startsWith("io.quarkus.http.ratelimit"))
            // the build warns neither about the rule nor about its principal key, because basic authentication is a
            // mechanism
            .assertLogRecords(records -> assertThat(records)
                    .filteredOn(r -> r.getLevel().intValue() >= Level.WARNING.intValue())
                    .extracting(LogRecord::getMessage).isEmpty());

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
    void identityIsStillResolved() {
        Map<String, String> alice = TestHttp.basic("alice", "alice");
        assertThat(http.get("/api/x", alice).statusCode()).isEqualTo(200);
        assertThat(http.get("/api/x", alice).statusCode()).isEqualTo(429);
        // an anonymous request is limited by its address, in a bucket of its own
        assertThat(http.get("/api/x").statusCode()).isEqualTo(200);
        assertThat(http.get("/api/x").statusCode()).isEqualTo(429);
        // a rejection names the user, and nobody for an anonymous request
        assertThat(rejections.principals).containsExactly(Optional.of("alice"), Optional.empty());
    }

    @Singleton
    public static class Rejections {

        final List<Optional<String>> principals = new CopyOnWriteArrayList<>();

        void onRejection(@Observes RateLimitRejectionInfo rejection) {
            principals.add(rejection.principal());
        }
    }
}
