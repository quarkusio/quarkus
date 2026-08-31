package io.quarkus.undertow.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.ext.web.Router;

/**
 * Tests that path normalization bypass vectors using matrix-param + dot-segment
 * combinations do not bypass security on wildcard routes when Undertow is present.
 */
public class WildcardPathNormalizationBypassTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestIdentityController.class, TestIdentityProvider.class, WildcardRouteHandler.class))
            .overrideConfigKey("quarkus.http.auth.permission.admin.paths", "/admin/*")
            .overrideConfigKey("quarkus.http.auth.permission.admin.policy", "authenticated");

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin");
    }

    @TestHTTPResource
    URL url;

    @Test
    public void testBaselineProtection() {
        assertStatusCode("/admin/secret", null, 401);
        assertStatusCode("/admin/secret", "admin:admin", 200);
        assertStatusCode("/admin/sub/data", null, 401);
        assertStatusCode("/admin/sub/data", "admin:admin", 200);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/admin/..;/secret",
            "/admin/..;x=1/secret",
            "/admin/sub/..;/data",
            "/admin/sub/..;/..;/outside",
    })
    public void testMatrixDotSegmentBypass(String path) {
        assertStatusCode(path, null, 401);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/admin/%252e%252e;/secret",
            "/admin/%25252e%25252e;/secret",
    })
    public void testDoubleEncodedMatrixDotSegmentBypass(String path) {
        assertStatusCode(path, null, 401);
    }

    private void assertStatusCode(String path, String credentials, int expectedStatus) {
        int status = rawHttpGet(path, credentials);
        assertEquals(expectedStatus, status, "Path: " + path + " (credentials=" + credentials + ")");
    }

    private int rawHttpGet(String path, String credentials) {
        try (var socket = new Socket(url.getHost(), url.getPort());
                var out = new PrintWriter(socket.getOutputStream(), false, StandardCharsets.UTF_8);
                var in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            out.print("GET " + path + " HTTP/1.1\r\n");
            out.print("Host: " + url.getHost() + ":" + url.getPort() + "\r\n");
            if (credentials != null) {
                out.print("Authorization: Basic "
                        + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)) + "\r\n");
            }
            out.print("Connection: close\r\n");
            out.print("\r\n");
            out.flush();
            String statusLine = in.readLine();
            if (statusLine == null) {
                throw new RuntimeException("No response from server for path: " + path);
            }
            return Integer.parseInt(statusLine.split(" ")[1]);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send request for path: " + path, e);
        }
    }

    @ApplicationScoped
    public static class WildcardRouteHandler {
        public void setup(@Observes Router router) {
            router.route("/admin/*").order(-1).handler(rc -> rc.response().end("admin-content"));
        }
    }
}
