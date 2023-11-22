package io.quarkus.vertx.http.security;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.time.Duration;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;

public class PathMatchingHttpSecurityPolicyTest {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final String APP_PROPS = "quarkus.http.auth.permission.authenticated.paths=/\n" +
            "quarkus.http.auth.permission.authenticated.policy=authenticated\n" +
            "quarkus.http.auth.permission.public.paths=/api*\n" +
            "quarkus.http.auth.permission.public.policy=permit\n" +
            "quarkus.http.auth.permission.foo.paths=/api/foo/bar\n" +
            "quarkus.http.auth.permission.foo.policy=authenticated\n" +
            "quarkus.http.auth.permission.inner-wildcard.paths=/api/*/bar\n" +
            "quarkus.http.auth.permission.inner-wildcard.policy=authenticated\n" +
            "quarkus.http.auth.permission.inner-wildcard2.paths=/api/next/*/prev\n" +
            "quarkus.http.auth.permission.inner-wildcard2.policy=authenticated\n" +
            "quarkus.http.auth.permission.inner-wildcard3.paths=/api/one/*/three/*\n" +
            "quarkus.http.auth.permission.inner-wildcard3.policy=authenticated\n" +
            "quarkus.http.auth.permission.inner-wildcard4.paths=/api/one/*/*/five\n" +
            "quarkus.http.auth.permission.inner-wildcard4.policy=authenticated\n" +
            "quarkus.http.auth.permission.inner-wildcard5.paths=/api/one/*/jamaica/*\n" +
            "quarkus.http.auth.permission.inner-wildcard5.policy=permit\n" +
            "quarkus.http.auth.permission.inner-wildcard6.paths=/api/*/sadly/*/dont-know\n" +
            "quarkus.http.auth.permission.inner-wildcard6.policy=deny\n" +
            "quarkus.http.auth.permission.baz.paths=/api/baz\n" +
            "quarkus.http.auth.permission.baz.policy=authenticated\n" +
            "quarkus.http.auth.permission.static-resource.paths=/static-file.html\n" +
            "quarkus.http.auth.permission.static-resource.policy=authenticated\n" +
            "quarkus.http.auth.permission.fubar.paths=/api/fubar/baz*\n" +
            "quarkus.http.auth.permission.fubar.policy=authenticated\n" +
            "quarkus.http.auth.permission.management.paths=/q/*\n" +
            "quarkus.http.auth.permission.management.policy=authenticated\n";
    private static WebClient client;

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
            .addClasses(TestIdentityController.class, TestIdentityProvider.class, PathHandler.class,
                    RouteHandler.class)
            .addAsResource("static-file.html", "META-INF/resources/static-file.html")
            .addAsResource(new StringAsset(APP_PROPS), "application.properties")).setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-smallrye-health", Version.getVersion()),
                    Dependency.of("io.quarkus", "quarkus-smallrye-openapi", Version.getVersion())));

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles().add("test", "test", "test");
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
    public void testInnerWildcardPath() {
        assurePath("/api/any-value/bar", 401);
        assurePath("/api/any-value/bar", 401);
        assurePath("/api/next/any-value/prev", 401);
        assurePath("/api/one/two/three/four", 401);
        assurePath("/api////any-value//////bar", 401);
        assurePath("/api/next///////any-value////prev", 401);
        assurePath("////api//one/two//three////four?door=wood", 401);
        assurePath("/api/one/three/four/five", 401);
        assurePath("/api/one/3/4/five", 401);
        assurePath("////api/one///3/4/five", 401);
        assurePath("/api/now/sadly/i/dont-know", 401);
        assurePath("/api/now/sadly///i/dont-know", 401);
        assurePath("/api/one/three/jamaica/five", 200);
        assurePath("/api/one/three/jamaica/football", 200);
        assurePath("/api/now/sally/i/dont-know", 200);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // path policy without wildcard
            "/api/foo//bar", "/api/foo///bar", "/api/foo////bar", "/api/foo/////bar", "//api/foo/bar", "///api/foo/bar",
            "////api/foo/bar", "//api//foo//bar", "//api/foo//bar",
            // path policy with wildcard
            "/api/fubar/baz", "/api/fubar/baz/", "/api/fubar/baz//", "/api/fubar/baz/.", "/api/fubar/baz////.",
            "/api/fubar/baz/bar",
            // routes defined for exact paths
            "/api/baz", "//api/baz", "///api////baz", "/api//baz",
            // zero length path
            "", "/?one=two",
            // empty segments only are match with path policy for '/'
            "/", "///", "////", "/////"
    })
    public void testEmptyPathSegments(String path) {
        assurePath(path, 401);
        assurePathAuthenticated(path);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/foo/./bar", "/../api/foo///bar", "/api/./foo/.///bar", "/api/foo/./////bar", "/api/fubar/baz/.",
            "/..///api/foo/bar", "////../../api/foo/bar", "/./api//foo//bar", "//api/foo/./bar",
            "/.", "/..", "/./", "/..//", "/.///", "/..////", "/./////"
    })
    public void testDotPathSegments(String path) {
        assurePath(path, 401);
        assurePathAuthenticated(path);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/static-file.html", "//static-file.html", "///static-file.html"
    })
    public void testStaticResource(String path) {
        assurePath(path, 401);
        assurePathAuthenticated(path);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "///q/openapi", "/q///openapi", "/q/openapi/", "/q/openapi///"
    })
    public void testOpenApiPath(String path) {
        assurePath(path, 401);
        assurePathAuthenticated(path, "openapi");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/q/health", "/q/health/live", "/q/health/ready", "//q/health", "///q/health", "///q///health",
            "/q/health/", "/q///health/", "/q///health////live"
    })
    public void testHealthCheckPaths(String path) {
        assurePath(path, 401);
        assurePathAuthenticated(path, "UP");
    }

    @Test
    public void testMiscellaneousPaths() {
        // /api/baz with segment indicating version shouldn't match /api/baz path policy
        assurePath("/api/baz;v=1.1", 200);
        // /api/baz/ is different resource than secured /api/baz, therefore request should succeed
        assurePath("/api/baz/", 200);
    }

    @ApplicationScoped
    public static class RouteHandler {
        public void setup(@Observes Router router) {
            router.route("/api/baz").order(-1).handler(rc -> rc.response().end("/api/baz response"));
        }
    }

    private void assurePath(String path, int expectedStatusCode) {
        assurePath(path, expectedStatusCode, null, false);
    }

    private void assurePathAuthenticated(String path) {
        assurePath(path, 200, null, true);
    }

    private void assurePathAuthenticated(String path, String body) {
        assurePath(path, 200, body, true);
    }

    private void assurePath(String path, int expectedStatusCode, String body, boolean auth) {
        var req = getClient().get(url.getPort(), url.getHost(), path);
        if (auth) {
            req.basicAuthentication("test", "test");
        }
        var result = req.send();
        await().atMost(REQUEST_TIMEOUT).until(result::isComplete);
        assertEquals(expectedStatusCode, result.result().statusCode(), path);
        if (body != null) {
            Assertions.assertTrue(result.result().bodyAsString().contains(body), path);
        }
    }

}
