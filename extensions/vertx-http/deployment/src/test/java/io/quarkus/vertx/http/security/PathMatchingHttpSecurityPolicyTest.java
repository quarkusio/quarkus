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
import org.junit.jupiter.api.extension.RegisterExtension;
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

public class PathMatchingHttpSecurityPolicyTest {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final String APP_PROPS = """
            quarkus.http.auth.permission.authenticated.paths=/
            quarkus.http.auth.permission.authenticated.policy=authenticated
            quarkus.http.auth.permission.public.paths=/api*
            quarkus.http.auth.permission.public.policy=permit
            quarkus.http.auth.permission.foo.paths=/api/foo/bar
            quarkus.http.auth.permission.foo.policy=authenticated
            quarkus.http.auth.permission.unsecured.paths=/api/public
            quarkus.http.auth.permission.unsecured.policy=permit
            quarkus.http.auth.permission.inner-wildcard.paths=/api/*/bar
            quarkus.http.auth.permission.inner-wildcard.policy=authenticated
            quarkus.http.auth.permission.inner-wildcard2.paths=/api/next/*/prev
            quarkus.http.auth.permission.inner-wildcard2.policy=authenticated
            quarkus.http.auth.permission.inner-wildcard3.paths=/api/one/*/three/*
            quarkus.http.auth.permission.inner-wildcard3.policy=authenticated
            quarkus.http.auth.permission.inner-wildcard4.paths=/api/one/*/*/five
            quarkus.http.auth.permission.inner-wildcard4.policy=authenticated
            quarkus.http.auth.permission.inner-wildcard5.paths=/api/one/*/jamaica/*
            quarkus.http.auth.permission.inner-wildcard5.policy=permit
            quarkus.http.auth.permission.inner-wildcard6.paths=/api/*/sadly/*/dont-know
            quarkus.http.auth.permission.inner-wildcard6.policy=deny
            quarkus.http.auth.permission.baz.paths=/api/baz
            quarkus.http.auth.permission.baz.policy=authenticated
            quarkus.http.auth.permission.static-resource.paths=/static-file.html
            quarkus.http.auth.permission.static-resource.policy=authenticated
            quarkus.http.auth.permission.fubar.paths=/api/fubar/baz*
            quarkus.http.auth.permission.fubar.policy=authenticated
            quarkus.http.auth.permission.management.paths=/q/*
            quarkus.http.auth.permission.management.policy=authenticated
            quarkus.http.auth.policy.shared1.roles.root=admin,user
            quarkus.http.auth.permission.shared1.paths=/secured/*
            quarkus.http.auth.permission.shared1.policy=shared1
            quarkus.http.auth.permission.shared1.shared=true
            quarkus.http.auth.policy.unshared1.roles-allowed=user
            quarkus.http.auth.permission.unshared1.paths=/secured/user/*
            quarkus.http.auth.permission.unshared1.policy=unshared1
            quarkus.http.auth.policy.unshared2.roles-allowed=admin
            quarkus.http.auth.permission.unshared2.paths=/secured/admin/*
            quarkus.http.auth.permission.unshared2.policy=unshared2
            quarkus.http.auth.permission.shared2.paths=/*
            quarkus.http.auth.permission.shared2.shared=true
            quarkus.http.auth.permission.shared2.policy=custom
            quarkus.http.auth.roles-mapping.root1=admin,user
            quarkus.http.auth.roles-mapping.admin1=admin
            quarkus.http.auth.roles-mapping.public1=public2
            """;
    private static WebClient client;

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
            .addClasses(TestIdentityController.class, TestIdentityProvider.class, PathHandler.class,
                    RouteHandler.class, CustomNamedPolicy.class)
            .addAsResource("static-file.html", "META-INF/resources/static-file.html")
            .addAsResource(new StringAsset(APP_PROPS), "application.properties"));

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

    @Test
    public void testMiscellaneousPaths() {
        // /api/baz with segment indicating version shouldn't match /api/baz path policy
        assurePath("/api/baz;v=1.1", 200);
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

    private void assurePathAuthenticated(String path, String body) {
        assurePath(path, 200, body, "test", null);
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
