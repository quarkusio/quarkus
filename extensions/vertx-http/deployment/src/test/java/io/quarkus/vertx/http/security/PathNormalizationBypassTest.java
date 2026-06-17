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
 * Tests for path normalization bypass vectors described in GHSA-qcxp-gm7m-4j5v.
 *
 * The vulnerability is a mismatch between the security layer (which uses Vert.x normalizedPath()
 * that only decodes unreserved RFC 3986 characters) and resource handlers that perform full URI
 * decoding. This mismatch allows attackers to craft paths that bypass security checks but still
 * resolve to protected resources.
 */
public class PathNormalizationBypassTest {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private static final String APP_PROPS = """
            quarkus.http.auth.permission.admin.paths=/api/admin/*
            quarkus.http.auth.permission.admin.policy=admin-policy
            quarkus.http.auth.policy.admin-policy.roles-allowed=admin
            quarkus.http.auth.permission.secret.paths=/api/secret/*
            quarkus.http.auth.permission.secret.policy=authenticated
            quarkus.http.auth.permission.static-secret.paths=/static-secret.html
            quarkus.http.auth.permission.static-secret.policy=authenticated
            quarkus.http.auth.permission.public.paths=/api/public/*
            quarkus.http.auth.permission.public.policy=permit
            """;

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> ShrinkWrap
            .create(JavaArchive.class)
            .addClasses(TestIdentityController.class, TestIdentityProvider.class, RouteHandlers.class)
            .addAsResource("static-secret.html", "META-INF/resources/static-secret.html")
            .addAsResource(new StringAsset(APP_PROPS), "application.properties"));

    private static WebClient client;

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin")
                .add("user", "user", "user");
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

    // ── Category 1: Percent-encoding of unreserved chars on REST endpoints ──
    // Vert.x normalizedPath() decodes unreserved chars (letters, digits, -, ., _, ~).
    // Both security and routing see the same decoded path. No bypass expected.

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/adm%69n/data",
            "/api/admi%6E/data",
            "/api/%61dmin/data",
            "/api/admin/d%61ta",
    })
    public void testUnreservedPercentEncodingOnAdminEndpoint(String path) {
        assurePath(path, 401, null);
        assurePath(path, 200, "admin");
        assurePath(path, 403, "user");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/secr%65t/data",
            "/api/s%65cret/data",
            "/api/secret/dat%61",
    })
    public void testUnreservedPercentEncodingOnSecretEndpoint(String path) {
        assurePath(path, 401, null);
        assurePath(path, 200, "user");
    }

    // ── Category 2: Encoded slash %2F ──
    // %2F (/) is a reserved char — NOT decoded by normalizedPath().
    // Security sees the literal string "%2F" as part of a segment.
    // Static handlers decode it to '/' which changes the path structure.

    @ParameterizedTest
    @ValueSource(strings = {
            "/api%2Fadmin%2Fdata",
            "/api%2Fadmin/data",
            "/api/admin%2Fdata",
    })
    public void testEncodedSlashOnRestEndpoint(String path) {
        var result = sendRequest(path, null);
        if (result.statusCode() == 200) {
            org.junit.jupiter.api.Assertions.assertFalse(
                    result.bodyAsString().contains("admin-data"),
                    "Encoded slash bypassed security on REST endpoint: " + path);
        }
    }

    @Test
    public void testEncodedSlashOnStaticResource() {
        assureNoSecretContent("/static-secret%2Fhtml");
        assureNoSecretContent("/%2Fstatic-secret.html");
    }

    // ── Category 3: Encoded backslash %5C ──
    // %5C (\) is NOT decoded by normalizedPath().
    // FileSystemStaticHandler does: URIDecoder.decodeURIComponent() then replace('\\', '/')

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/admin%5Cdata",
            "/api%5Cadmin/data",
    })
    public void testEncodedBackslashOnRestEndpoint(String path) {
        var result = sendRequest(path, null);
        if (result.statusCode() == 200) {
            org.junit.jupiter.api.Assertions.assertFalse(
                    result.bodyAsString().contains("admin-data"),
                    "Encoded backslash bypassed security on REST endpoint: " + path);
        }
    }

    @Test
    public void testEncodedBackslashOnStaticResource() {
        assureNoSecretContent("/static-secret%5Chtml");
    }

    // ── Category 4: Null byte %00 ──
    // %00 is NOT decoded by normalizedPath() (not unreserved).
    // Modern Java doesn't truncate on null bytes.

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/admin%00/data",
            "/api/admin/data%00",
            "/api/admin%00suffix/data",
    })
    public void testNullByteOnRestEndpoint(String path) {
        var result = sendRequest(path, null);
        if (result.statusCode() == 200) {
            org.junit.jupiter.api.Assertions.assertFalse(
                    result.bodyAsString().contains("admin-data"),
                    "Null byte bypassed security on REST endpoint: " + path);
        }
    }

    @Test
    public void testNullByteOnStaticResource() {
        assureNoSecretContent("/static-secret.html%00");
        assureNoSecretContent("/static-secret%00.html");
    }

    // ── Category 5: Encoded semicolon %3B (matrix param smuggling) ──
    // Semicolon (;) is reserved — NOT decoded by normalizedPath().
    // pathWithoutMatrixParams() only strips literal ';', not encoded '%3B'.
    // After full decode, %3B becomes ';' which changes matrix param parsing.

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/admin%3Bbypass=true/data",
            "/api/admin%3b/data",
            "/api/admin/data%3Bv=1",
    })
    public void testEncodedSemicolonOnRestEndpoint(String path) {
        var result = sendRequest(path, null);
        if (result.statusCode() == 200) {
            org.junit.jupiter.api.Assertions.assertFalse(
                    result.bodyAsString().contains("admin-data"),
                    "Encoded semicolon bypassed security on REST endpoint: " + path);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/secret%3Bbypass=true/data",
            "/api/secret%3b/data",
    })
    public void testEncodedSemicolonOnSecretEndpoint(String path) {
        var result = sendRequest(path, null);
        if (result.statusCode() == 200) {
            org.junit.jupiter.api.Assertions.assertFalse(
                    result.bodyAsString().contains("secret-data"),
                    "Encoded semicolon bypassed security on secret endpoint: " + path);
        }
    }

    @Test
    public void testEncodedSemicolonOnStaticResource() {
        assureNoSecretContent("/static-secret.html%3Bv=1");
        assureNoSecretContent("/static-secret%3B.html");
    }

    // ── Category 6: Double encoding %25xx ──
    // %25 = literal '%'. Not unreserved, stays as %25.
    // Without a decode loop, double-encoded chars survive the security check
    // but may be decoded by downstream handlers.

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/adm%2569n/data",
            "/api/admin%252Fdata",
            "/api/adm%252569n/data",
    })
    public void testDoubleEncodingOnRestEndpoint(String path) {
        var result = sendRequest(path, null);
        if (result.statusCode() == 200) {
            org.junit.jupiter.api.Assertions.assertFalse(
                    result.bodyAsString().contains("admin-data"),
                    "Double encoding bypassed security on REST endpoint: " + path);
        }
    }

    @Test
    public void testDoubleEncodingOnStaticResource() {
        assureNoSecretContent("/static-secret%252Ehtml");
        assureNoSecretContent("/static%252Dsecret.html");
    }

    // ── Category 7: Encoded dot segments ──
    // Period (.) is unreserved -> decoded by normalizedPath(), then removeDots() handles ..
    // Both security and routing see the same normalized path, so no bypass is possible.

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/public/%2e%2e/secret/data", // normalizes to /api/secret/data
            "/api/public/%2e%2e/admin/data", // normalizes to /api/admin/data
            "/%2e%2e/%2e%2e/api/secret/data", // normalizes to /api/secret/data
    })
    public void testEncodedDotSegmentsOnRestEndpoint(String path) {
        assurePath(path, 401, null);
    }

    @Test
    public void testEncodedDotSegmentsEscapingPolicyPrefix() {
        // /api/%2e%2e/secret/data normalizes to /secret/data — OUTSIDE the /api/secret/* policy.
        // No policy matches, no route matches → 404. This is not a bypass because no content is served.
        // Both security and routing agree on the normalized path.
        assurePath("/api/%2e%2e/secret/data", 404, null);
    }

    @Test
    public void testEncodedDotSegmentsOnStaticResource() {
        assurePath("/something/../static-secret.html", 401, null);
        assurePath("/something/%2e%2e/static-secret.html", 401, null);
    }

    // ── Category 8: Combined attack vectors ──

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/adm%69n;v=1/data",
            "/api/admin%2Fdata;v=1",
            "/api%2Fadmin/data;bypass",
            "/api/public/../admin/d%61ta",
            "/api/admin%3Bbypass=true/d%61ta",
            "/api/adm%2569n%3Bv=1/data",
            "/api/admin%2Fdata%3Bv=1",
            "/api%5Cadmin%3Bbypass/data",
    })
    public void testCombinedAttacksOnRestEndpoint(String path) {
        var result = sendRequest(path, null);
        if (result.statusCode() == 200) {
            org.junit.jupiter.api.Assertions.assertFalse(
                    result.bodyAsString().contains("admin-data"),
                    "Combined attack bypassed security on REST endpoint: " + path);
        }
    }

    // ── Baseline: confirm normal access control works ──

    @Test
    public void testBaselineAdminEndpoint() {
        assurePath("/api/admin/data", 401, null);
        assurePath("/api/admin/data", 200, "admin");
        assurePath("/api/admin/data", 403, "user");
    }

    @Test
    public void testBaselineSecretEndpoint() {
        assurePath("/api/secret/data", 401, null);
        assurePath("/api/secret/data", 200, "user");
        assurePath("/api/secret/data", 200, "admin");
    }

    @Test
    public void testBaselinePublicEndpoint() {
        assurePath("/api/public/data", 200, null);
    }

    @Test
    public void testBaselineStaticResource() {
        assurePath("/static-secret.html", 401, null);
        assurePath("/static-secret.html", 200, "user");
    }

    // ── Helpers ──

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

    private void assureNoSecretContent(String path) {
        var result = sendRequest(path, null);
        if (result.statusCode() == 200) {
            String body = result.bodyAsString();
            org.junit.jupiter.api.Assertions.assertFalse(
                    body != null && body.contains("secret"),
                    "Path '" + path + "' returned secret content without authentication (status "
                            + result.statusCode() + ")");
        }
    }

    private io.vertx.ext.web.client.HttpResponse<io.vertx.core.buffer.Buffer> sendRequest(String path, String auth) {
        var req = getClient().get(url.getPort(), url.getHost(), path);
        if (auth != null) {
            req.basicAuthentication(auth, auth);
        }
        var result = req.send();
        await().atMost(REQUEST_TIMEOUT).until(result::isComplete);
        return result.result();
    }

    @ApplicationScoped
    public static class RouteHandlers {
        public void setup(@Observes Router router) {
            router.route("/api/admin/data").order(-1).handler(rc -> rc.response().end("admin-data"));
            router.route("/api/secret/data").order(-1).handler(rc -> rc.response().end("secret-data"));
            router.route("/api/public/data").order(-1).handler(rc -> rc.response().end("public-data"));
        }
    }
}
