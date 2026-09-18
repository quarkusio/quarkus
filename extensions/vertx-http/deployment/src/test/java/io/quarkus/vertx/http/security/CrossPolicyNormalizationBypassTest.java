package io.quarkus.vertx.http.security;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;

/**
 * Tests that aggressive path normalization cannot cause cross-policy confusion.
 * <p>
 * When both /admin/* (authenticated) and /public/* (permit) are configured,
 * an attacker could use /admin/..;/public/data which normalizes to /public/data,
 * matching the permit policy while the router still dispatches into /admin/*.
 * <p>
 * Both normalization forms must be matched and their policies combined.
 */
public class CrossPolicyNormalizationBypassTest {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private static final String APP_PROPS = """
            quarkus.http.auth.permission.admin.paths=/admin/*
            quarkus.http.auth.permission.admin.policy=authenticated
            quarkus.http.auth.permission.public.paths=/public/*
            quarkus.http.auth.permission.public.policy=permit
            quarkus.http.auth.permission.public-secret.paths=/public/secret
            quarkus.http.auth.permission.public-secret.policy=authenticated
            quarkus.http.auth.permission.admin-health.paths=/admin/health
            quarkus.http.auth.permission.admin-health.policy=permit
            """;

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(TestIdentityController.class, TestIdentityProvider.class, RouteHandler.class)
            .addAsResource(new StringAsset(APP_PROPS), "application.properties"));

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
        assurePath("/public/data", 200, null);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/admin/..;/public/data",
            "/admin/..;x=1/public/data",
            "/admin/sub/..;/..;/public/data",
    })
    public void testCrossPolicyBypass(String path) {
        assurePath(path, 401, null);
    }

    @Test
    public void testOverlappingPolicySpecificWins() {
        // /public/secret is protected (authenticated) even though /public/* is permit
        assurePath("/public/secret", 401, null);
        assurePath("/public/secret", 200, "admin");
        // other paths under /public/* remain permitted
        assurePath("/public/data", 200, null);

        // /admin/health is permit even though /admin/* is authenticated
        assurePath("/admin/health", 200, null);
        // other paths under /admin/* remain protected
        assurePath("/admin/other", 401, null);
        assurePath("/admin/other", 200, "admin");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/public/..;/public/secret",
            "/admin/..;/public/secret",
            "/public/sub/..;/secret",
    })
    public void testOverlappingPolicyBypass(String path) {
        // normalization must not weaken the specific /public/secret authenticated policy
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
    public static class RouteHandler {
        public void setup(@Observes Router router) {
            router.route("/admin/health").order(-2).handler(rc -> rc.response().end("admin-health"));
            router.route("/admin/*").order(-1).handler(rc -> rc.response().end("admin-content"));
            router.route("/public/secret").order(-2).handler(rc -> rc.response().end("public-secret"));
            router.route("/public/*").order(-1).handler(rc -> rc.response().end("public-content"));
        }
    }
}
