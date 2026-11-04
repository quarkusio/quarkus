package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import jakarta.inject.Inject;

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
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class)))
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
            .assertLogRecords(records -> {
                assertThat(records).anySatisfy(r -> {
                    assertThat(r.getLevel()).isEqualTo(Level.WARNING);
                    assertThat(r.getMessage()).isEqualTo("Rate limit rule 'api' runs after authentication but "
                            + "quarkus.http.auth.proactive=false: authentication is now forced for every request to "
                            + "its paths, using the mechanisms selected by path; mechanisms selected by REST "
                            + "annotations are not known at this point and are not applied");
                });
                // basic authentication is a mechanism
                assertThat(records).noneSatisfy(r -> assertThat(r.getMessage())
                        .contains("no HttpAuthenticationMechanism bean is present"));
            });

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
    void identityIsStillResolved() {
        Map<String, String> alice = TestHttp.basic("alice", "alice");
        assertThat(http.get("/api/x", alice).statusCode()).isEqualTo(200);
        assertThat(http.get("/api/x", alice).statusCode()).isEqualTo(429);
        // an anonymous request is limited by its address, in a bucket of its own
        assertThat(http.get("/api/x").statusCode()).isEqualTo(200);
        assertThat(http.get("/api/x").statusCode()).isEqualTo(429);
    }
}
