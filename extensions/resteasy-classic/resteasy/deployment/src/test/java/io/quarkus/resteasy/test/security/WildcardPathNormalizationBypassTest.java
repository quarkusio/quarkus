package io.quarkus.resteasy.test.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpClientRequest;

/**
 * Tests that path normalization bypass vectors using matrix-param + dot-segment
 * combinations do not bypass security on wildcard routes when RESTEasy Classic
 * is present.
 */
public class WildcardPathNormalizationBypassTest {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestIdentityProvider.class, TestIdentityController.class, WildcardRouteHandler.class))
            .overrideConfigKey("quarkus.http.auth.permission.admin.paths", "/admin/*")
            .overrideConfigKey("quarkus.http.auth.permission.admin.policy", "authenticated");

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin");
    }

    @TestHTTPResource
    URL url;

    @Inject
    Vertx vertx;

    @Test
    public void testBaselineProtection() {
        assurePath("/admin/secret", 401, false);
        assurePath("/admin/secret", 200, true);
        assurePath("/admin/sub/data", 401, false);
        assurePath("/admin/sub/data", 200, true);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/admin/..;/secret",
            "/admin/..;x=1/secret",
            "/admin/sub/..;/data",
            "/admin/sub/..;/..;/outside",
    })
    public void testMatrixDotSegmentBypass(String path) {
        assurePath(path, 401, false);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/admin/%252e%252e;/secret",
            "/admin/%25252e%25252e;/secret",
    })
    public void testDoubleEncodedMatrixDotSegmentBypass(String path) {
        assurePath(path, 401, false);
    }

    private void assurePath(String path, int expectedStatusCode, boolean auth) {
        var httpClient = vertx.createHttpClient();
        try {
            httpClient
                    .request(HttpMethod.GET, url.getPort(), url.getHost(), path)
                    .map(r -> {
                        if (auth) {
                            r.putHeader("Authorization",
                                    "Basic " + java.util.Base64.getEncoder()
                                            .encodeToString("admin:admin".getBytes()));
                        }
                        return r;
                    })
                    .flatMap(HttpClientRequest::send)
                    .invoke(r -> assertEquals(expectedStatusCode, r.statusCode(),
                            "Path: " + path + " (auth=" + auth + ")"))
                    .await()
                    .atMost(REQUEST_TIMEOUT);
        } finally {
            httpClient
                    .close()
                    .await()
                    .atMost(REQUEST_TIMEOUT);
        }
    }

    @ApplicationScoped
    public static class WildcardRouteHandler {
        public void setup(@Observes Router router) {
            router.route("/admin/*").order(-1).handler(rc -> rc.response().end("admin-content"));
        }
    }
}
