package io.quarkus.vertx.http.security;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;

public abstract class PathMatchingHttpSecurityPolicyTest {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static WebClient client;

    protected static QuarkusUnitTest createQuarkusUnitTest(String applicationProperties, Class<?>... additionalTestClasses) {
        return new QuarkusUnitTest().setArchiveProducer(() -> {
            var javaArchive = ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestIdentityController.class, TestIdentityProvider.class, PathHandler.class,
                            RouteHandler.class, CustomNamedPolicy.class)
                    .addAsResource("static-file.html", "META-INF/resources/static-file.html")
                    .addAsResource(new StringAsset(applicationProperties), "application.properties");
            if (additionalTestClasses.length > 0) {
                javaArchive.addClasses(additionalTestClasses);
            }
            return javaArchive;
        });
    }

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles()
                .add("test", "test", "test")
                .add("admin", "admin", "admin")
                .add("user", "user", "user")
                .add("admin1", "admin1", "admin1")
                .add("root1", "root1", "root1")
                .add("root", "root", "root")
                .add("public1", "public1", "public1");
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

    @Test
    public void testInnerWildcardPathWithMatrix() {
        assurePath("/api;/any-value;/bar;", 401);
        assurePath("/api;param/any-value;param/bar;param", 401);
        assurePath("/api;/any-value;/bar;", 401);
        assurePath("/api;param/any-value;param/bar;param", 401);
        assurePath("/api;/next;/any-value;/prev;", 401);
        assurePath("/api;param/next;param/any-value;param/prev;param", 401);
        assurePath("/api;/one;/two;/three;/four;", 401);
        assurePath("/api;param/one;param/two;param/three;param/four;param", 401);
        assurePath("/api;////any-value;//////bar;", 401);
        assurePath("/api;param////any-value;param//////bar;param", 401);
        assurePath("/api;/next;///////any-value;////prev;", 401);
        assurePath("/api;param/next;param///////any-value;param////prev;param", 401);
        assurePath("////api;//one;/two;//three;////four;?door=wood", 401);
        assurePath("////api;param//one;param/two;param//three;param////four;param?door=wood", 401);
        assurePath("/api;/one;/three;/four;/five;", 401);
        assurePath("/api;param/one;param/three;param/four;param/five;param", 401);
        assurePath("/api;/one;/3;/4;/five;", 401);
        assurePath("/api;param/one;param/3;param/4;param/five;param", 401);
        assurePath("////api;/one;///3;/4;/five;", 401);
        assurePath("////api;param/one;param///3;param/4;param/five;param", 401);
        assurePath("/api;/now;/sadly;/i;/dont-know;", 401);
        assurePath("/api;param/now;param/sadly;param/i;param/dont-know;param", 401);
        assurePath("/api;/now;/sadly;///i;/dont-know;", 401);
        assurePath("/api;param/now;param/sadly;param///i;param/dont-know;param", 401);
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
            // path policy without wildcard
            "/api;/foo;//bar;", "/api;/foo;///bar;", "/api;/foo;////bar;", "/api;/foo;/////bar;", "//api;/foo;/bar;",
            "///api;/foo;/bar;",
            "////api;/foo;/bar;", "//api;//foo;//bar;", "//api;/foo;//bar;",
            "/api;param/foo;param//bar;param", "/api;param/foo;param///bar;param", "/api;param/foo;param////bar;param",
            "/api;param/foo;param/////bar;param", "//api;param/foo;param/bar;param", "///api;param/foo;param/bar;param",
            "////api;param/foo;param/bar;param", "//api;param//foo;param//bar;param", "//api;param/foo;param//bar;param",
            // path policy with wildcard
            "/api;/fubar;/baz;", "/api;/fubar;/baz;/;", "/api;/fubar;/baz;//;", "/api;/fubar;/baz;/.", "/api;/fubar;/baz;////.",
            "/api;/fubar;/baz;/bar;",
            "/api;param/fubar;param/baz;param", "/api;param/fubar;param/baz;param/", "/api;param/fubar;param/baz;param//",
            "/api;param/fubar;param/baz;param/.", "/api;param/fubar;param/baz;param////.",
            "/api;param/fubar;param/baz;param/bar;param",
            // routes defined for exact paths
            "/api;/baz;", "//api;/baz;", "///api;////baz;", "/api;//baz;",
            "/api;param/baz;param", "//api;param/baz;param", "///api;param////baz;param", "/api;param//baz;param",
            // zero length path
            "/;?one=two",
            "/;param?one=two",
            // empty segments only are match with path policy for '/'
            "/;", "///;", "////;", "/////;",
            "/;param", "///;param", "////;param", "/////;param"
    })
    public void testEmptyPathSegmentsWithMatrix(String path) {
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
            "/api;/foo;/./bar;", "/../api;/foo;///bar;", "/api;/./foo;/.///bar;", "/api;/foo;/./////bar;",
            "/api;/fubar;/baz;/.",
            "/api;param/foo;param/./bar;param", "/../api;param/foo;param///bar;param", "/api;param/./foo;param/.///bar;param",
            "/api;param/foo;param/./////bar;param", "/api;param/fubar;param/baz;param/.",
            "/..///api;/foo;/bar;", "////../../api;/foo;/bar;", "/./api;//foo;//bar;", "//api;/foo;/./bar;",
            "/..///api;param/foo;param/bar;param", "////../../api;param/foo;param/bar;param",
            "/./api;param//foo;param//bar;param",
            "//api;param/foo;param/./bar;param"
    })
    public void testDotPathSegmentsWithMatrix(String path) {
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
            "/static-file.html;", "//static-file.html;", "///static-file.html;",
            "/static-file.html;param", "//static-file.html;param", "///static-file.html;param"
    })
    public void testStaticResourceWithParam(String path) {
        assurePath(path, 401);
        assurePathAuthenticated(path);
    }

    @Test
    public void testExactPathWithMatrixParams() {
        // /api/baz policy = authenticated; matrix params must not bypass it
        assurePath("/api/baz;v=1.1", 401);
        assurePath("/api/baz;", 401);
        assurePath("/api/baz;a=1;b=2", 401);
        assurePathAuthenticated("/api/baz;v=1.1");
        assurePathAuthenticated("/api/baz;");

        // /api/foo/bar policy = authenticated; matrix on intermediate and last segments
        assurePath("/api/foo;x=1/bar", 401);
        assurePath("/api/foo/bar;y=2", 401);
        assurePath("/api/foo;x=1/bar;y=2", 401);
        assurePathAuthenticated("/api/foo;x=1/bar");
        assurePathAuthenticated("/api/foo/bar;y=2");
        assurePathAuthenticated("/api/foo;x=1/bar;y=2");
    }

    @Test
    public void testEncodedSemicolon() {
        // /api/baz policy = authenticated; matrix params must not bypass it
        assurePath("/api/baz;v=1.1", 401);
        assurePathAuthenticated("/api/baz;v=1.1");
        // %3B is not matrix parameter therefore requires a dedicated policy
        assurePath("/api/baz%3Bv=1.1", 401);
        assurePathAuthenticated("/api/baz%3Bv=1.1");
    }

    @Test
    public void testWildcardSuffixPathWithMatrixParams() {
        // /api/fubar/baz* policy = authenticated

        assurePath("/api/fubar/baz;v=1", 401);
        assurePath("/api/fubar;x=1/baz", 401);
        assurePath("/api/fubar/baz/extra;v=1", 401);
        assurePathAuthenticated("/api/fubar/baz;v=1");
        assurePathAuthenticated("/api/fubar;x=1/baz");
    }

    @Test
    public void testStaticResourceWithMatrixParams() {
        // /static-file.html policy = authenticated
        assurePath("/static-file.html;v=1", 401);
        assurePathAuthenticated("/static-file.html;v=1");
    }

    @Test
    public void testRootPathWithMatrixParams() {
        // / policy = authenticated
        assurePath("/;v=1", 401);
        assurePath("/;", 401);
        assurePathAuthenticated("/;v=1");
    }

    @Test
    public void testPermittedPathWithMatrixParams() {
        // /api* policy = permit; must still be permitted with matrix params
        assurePath("/api/something;v=1", 200);
        assurePath("/api;v=1/something", 200);
    }

    @Test
    public void testMatrixParamsWithMultipleSlashes() {
        assurePath("//api;x=1//baz;y=2", 401);
        assurePath("///api;x=1/foo;y=2/bar;z=3", 401);
        assurePathAuthenticated("//api;x=1//baz;y=2");
        assurePathAuthenticated("///api;x=1/foo;y=2/bar;z=3");
    }

    @Test
    public void testMiscellaneousPaths() {
        // /api/baz/ is different resource than secured /api/baz, but we secure both when there is not more specific exact path pattern
        assurePath("/api/baz/", 401);
    }

    @Test
    public void testCustomSharedPermission() {
        assurePath("/", 401, null, null, null);
        assurePath("/", 200, null, "test", null);
        assurePath("/", 403, null, "test", "deny-header");
        assurePath("/api/one/anything/jamaica/anything", 200, null, null, null);
        assurePath("/api/one/anything/jamaica/anything", 401, null, null, "deny-header");
    }

    @Test
    public void testRoleMappingSharedPermission() {
        assurePath("/secured", 401, null, null, null);
        assurePath("/secured", 200, null, "test", null);
        assurePath("/secured/all", 401, null, null, null);
        assurePath("/secured/all", 200, null, "test", null);
        assurePath("/secured/all", 200, null, "root", null);
        assurePath("/secured/all", 200, null, "root1", null);
        assurePath("/secured/all", 200, null, "admin", null);
        assurePath("/secured/user", 403, null, "test", null);
        assurePath("/secured/user", 403, null, "admin", null);
        assurePath("/secured/user", 403, null, "admin1", null);
        assurePath("/secured/user", 200, null, "root", null);
        assurePath("/secured/user", 200, null, "root1", null);
        assurePath("/secured/user", 200, null, "user", null);
        assurePath("/secured/admin", 403, null, "user", null);
        assurePath("/secured/admin", 403, null, "test", null);
        assurePath("/secured/admin", 200, null, "admin", null);
        assurePath("/secured/admin", 200, null, "admin1", null);
        assurePath("/secured/admin", 200, null, "root", null);
        assurePath("/secured/admin", 200, null, "root1", null);
    }

    @Test
    public void testMultipleSharedPermissions() {
        assurePath("/secured/user", 200, null, "root", null);
        assurePath("/secured/user", 403, null, "root", "deny-header");
    }

    @Test
    public void testRolesMappingOnPublicPath() {
        // here no HTTP Security policy that requires authentication is applied, and we want to check that identity
        // is still augmented
        assurePath("/api/public", 200, null, "public1", null);
        assurePath("/api/public", 403, null, "root1", null);
    }

    @ApplicationScoped
    public static class RouteHandler {
        public void setup(@Observes Router router) {
            router.route("/api/baz").order(-1).handler(rc -> rc.response().end("/api/baz response"));
            router.route("/api/public").order(-1).handler(rc -> {
                if (rc.user() instanceof QuarkusHttpUser user && user.getSecurityIdentity() != null
                        && user.getSecurityIdentity().hasRole("public2")) {
                    rc.response().end("/api/public");
                } else {
                    rc.fail(new ForbiddenException());
                }
            });
        }
    }

    private void assurePath(String path, int expectedStatusCode) {
        assurePath(path, expectedStatusCode, null, null, null);
    }

    private void assurePathAuthenticated(String path) {
        assurePath(path, 200, null, "test", null);
    }

    private void assurePath(String path, int expectedStatusCode, String body, String auth, String header) {
        var req = getClient().get(url.getPort(), url.getHost(), path);
        if (auth != null) {
            req.basicAuthentication(auth, auth);
        }
        if (header != null) {
            req.putHeader(header, header);
        }
        var result = req.send();
        await().atMost(REQUEST_TIMEOUT).until(result::isComplete);
        assertEquals(expectedStatusCode, result.result().statusCode(), path);
        if (body != null) {
            Assertions.assertTrue(result.result().bodyAsString().contains(body), path);
        }
    }

    @Singleton
    public static class CustomNamedPolicy implements HttpSecurityPolicy {
        @Override
        public Uni<CheckResult> checkPermission(RoutingContext event, Uni<SecurityIdentity> identity,
                AuthorizationRequestContext requestContext) {
            if (event.request().getHeader("deny-header") != null) {
                return Uni.createFrom().item(CheckResult.DENY);
            }
            return CheckResult.permit();
        }

        @Override
        public String name() {
            return "custom";
        }
    }

}
