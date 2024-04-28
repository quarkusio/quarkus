package io.quarkus.resteasy.reactive.server.test.security;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

public class JakartaRestResourceHttpPermissionTest {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final String APP_PROPS = "quarkus.http.auth.permission.foo.paths=/api/foo,/api/foo/\n" +
            "quarkus.http.auth.permission.foo.policy=authenticated\n" +
            "quarkus.http.auth.permission.bar.paths=api/bar*\n" +
            "quarkus.http.auth.permission.bar.policy=authenticated\n" +
            "quarkus.http.auth.permission.baz-fum-pub.paths=/api/baz/fum\n" +
            "quarkus.http.auth.permission.baz-fum-pub.policy=permit\n" +
            "quarkus.http.auth.permission.baz-fum-deny.paths=/api/baz/fum/\n" +
            "quarkus.http.auth.permission.baz-fum-deny.policy=authenticated\n" +
            "quarkus.http.auth.permission.baz-fum.paths=/api/baz/fum*\n" +
            "quarkus.http.auth.permission.baz-fum.policy=authenticated\n" +
            "quarkus.http.auth.permission.root.paths=/\n" +
            "quarkus.http.auth.permission.root.policy=authenticated\n" +
            "quarkus.http.auth.permission.fragment.paths=/#stuff,/#stuff/\n" +
            "quarkus.http.auth.permission.fragment.policy=authenticated\n" +
            "quarkus.http.auth.permission.jax-rs.paths=jax-rs\n" +
            "quarkus.http.auth.permission.jax-rs.policy=admin-role\n" +
            "quarkus.http.auth.policy.admin-role.roles-allowed=admin";
    private static WebClient client;

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestIdentityProvider.class, TestIdentityController.class, ApiResource.class,
                            RootResource.class, PublicResource.class, JaxRsResource.class)
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties"));

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin")
                .add("test", "test", "test");
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

    @ParameterizedTest
    @ValueSource(strings = {
            // path without wildcard, with leading slashes in both policy and @Path
            "////api/foo", "/api/foo", "/api//foo", "/api//foo", "/api///foo", "/api/foo/", "/api/foo///",
            "/api/foo///.", "/api/foo/./",
            // path with wildcard, without leading slashes in both policy and @Path
            "////api/bar", "/api///bar", "/api//bar", "/api/bar", "/api/bar/", "/api/bar/irish",
            "/api/bar///irish", "/api/bar///irish/.", "/../api/bar///irish/.",
            // combination of permit and authenticated policies, paths are resolved to /api/baz/fum/ and auth required
            "/api/baz/fum/", "/api//baz/fum//", "/api//baz/fum/."
    })
    public void testEmptyPathSegments(String path) {
        assurePath(path, 401);

        assurePathAuthenticated(path, getLastNonEmptySegmentContent(path));
    }

    @ParameterizedTest
    @ValueSource(strings = { "/", "///", "/?stuff", "/#stuff/", "" })
    public void testRootPath(String path) {
        assurePath(path, 401);
        assurePathAuthenticated(path);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/one/", "///two", "/three?stuff", "/four#stuff", "/.////five" })
    public void testNotSecuredPaths(String path) {
        // negative testing - all paths are public unless auth policy is applied
        assurePathAuthenticated(path);
    }

    @Test
    public void testJaxRsRolesHttpSecurityPolicy() {
        // insufficient role, expected admin
        assurePath("/jax-rs", 401);
        assurePath("///jax-rs///", 401);

        assurePath("/jax-rs", 200, "admin", true, "admin");
    }

    private static String getLastNonEmptySegmentContent(String path) {
        while (path.endsWith("/") || path.endsWith(".")) {
            path = path.substring(0, path.length() - 1);
        }
        return path.substring(path.lastIndexOf('/') + 1);
    }

    @Path("jax-rs")
    public static class JaxRsResource {

        @Inject
        SecurityIdentity identity;

        @GET
        public String getPrincipalName() {
            return identity.getPrincipal().getName();
        }
    }

    @Path("/api")
    public static class ApiResource {

        @GET
        @Path("/foo")
        public String foo() {
            return "foo";
        }

        @GET
        @Path("bar")
        public String bar() {
            return "bar";
        }

        @GET
        @Path("bar/irish")
        public String irishBar() {
            return "irish";
        }

        @GET
        @Path("baz/fum")
        public String bazFum() {
            return "fum";
        }

    }

    @Path("/")
    public static class RootResource {

        @GET
        public String get() {
            return "root";
        }

        @Path("#stuff")
        @GET
        public String fragment() {
            return "#stuff";
        }

    }

    @Path("/")
    public static class PublicResource {

        @Path("one")
        @GET
        public String one() {
            return "one";
        }

        @Path("/two")
        @GET
        public String two() {
            return "two";
        }

        @Path("/three")
        @GET
        public String three() {
            return "three";
        }

        @Path("four")
        @GET
        public String four() {
            return "four";
        }

        @Path("four#stuff")
        @GET
        public String fourFragment() {
            return "four#stuff";
        }

        @Path("five")
        @GET
        public String five() {
            return "five";
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
        assurePath(path, expectedStatusCode, body, auth, "test");
    }

    private void assurePath(String path, int expectedStatusCode, String body, boolean auth, String username) {
        var req = getClient().get(url.getPort(), url.getHost(), path);
        if (auth) {
            req.basicAuthentication(username, username);
        }
        var result = req.send();
        await().atMost(REQUEST_TIMEOUT).until(result::isComplete);
        assertEquals(expectedStatusCode, result.result().statusCode(), path);
        if (body != null) {
            Assertions.assertTrue(result.result().bodyAsString().contains(body), path);
        }
    }
}
