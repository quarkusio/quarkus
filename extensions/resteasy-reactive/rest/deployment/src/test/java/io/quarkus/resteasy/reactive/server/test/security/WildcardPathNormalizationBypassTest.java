package io.quarkus.resteasy.reactive.server.test.security;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;

/**
 * Tests that path normalization bypass vectors using matrix-param + dot-segment
 * combinations do not bypass security on wildcard routes when RESTEasy Reactive
 * is present.
 */
public class WildcardPathNormalizationBypassTest {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestIdentityController.class, TestIdentityProvider.class, WildcardRouteHandler.class))
            .overrideConfigKey("quarkus.http.auth.permission.admin.paths", "/admin/*")
            .overrideConfigKey("quarkus.http.auth.permission.admin.policy", "authenticated");

    private static WebClient client;

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin");
    }

    @AfterAll
    public static void cleanup() {
        if (client != null) {
            client.close();
        }
    }

    @Inject
    Vertx vertx;

    @TestHTTPResource
    URL url;

    private WebClient getClient() {
        if (client == null) {
            client = WebClient.create(vertx);
        }
        return client;
    }

    @Test
    public void testBaselineProtection() {
        assurePath("/admin/secret", 401, null);
        assurePath("/admin/secret", 200, "admin");
        assurePath("/admin/sub/data", 401, null);
        assurePath("/admin/sub/data", 200, "admin");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/admin/..;/secret",
            "/admin/..;x=1/secret",
            "/admin/sub/..;/data",
            "/admin/sub/..;/..;/outside",
    })
    public void testMatrixDotSegmentBypass(String path) {
        assurePath(path, 401, null);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/admin/%252e%252e;/secret",
            "/admin/%25252e%25252e;/secret",
    })
    public void testDoubleEncodedMatrixDotSegmentBypass(String path) {
        assurePath(path, 401, null);
    }

    private void assurePath(String path, int expectedStatusCode, String auth) {
        var req = getClient().get(url.getPort(), url.getHost(), path);
        if (auth != null) {
            req.basicAuthentication(auth, auth);
        }
        var result = req.send();
        await().atMost(REQUEST_TIMEOUT).until(result::isComplete);
        assertEquals(expectedStatusCode, result.result().statusCode(),
                "Path: " + path + " (auth=" + auth + ")");
    }

    @ApplicationScoped
    public static class WildcardRouteHandler {
        public void setup(@Observes Router router) {
            router.route("/admin/*").order(-1).handler(rc -> rc.response().end("admin-content"));
        }
    }
}
