package io.quarkus.resteasy.reactive.server.test.security;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.security.Authenticated;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

public class JakartaRestAuthenticationWithMatrixTest {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private static WebClient client;

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestIdentityProvider.class, TestIdentityController.class, ApiResource.class,
                            ApiWithMatrixResource.class, ApiWithEncodedSemicolonResource.class));

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
            "/api", "/api/service",
            "/api;", "/api;/service;",
            "/api;a", "/api;/service;a",
            "/api-with-percent-encoded%3Ba"
    })
    public void testNotAuthenticated(String path) {
        assureAuthenticationFailure(path);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api", "/api/service",
            "/api;", "/api;/service;",
            "/api;a", "/api;/service;a",
            "/api-with-percent-encoded%3Ba"
    })
    public void testAuthenticated(String path) {
        assureAuthenticationSuccess(path);
    }

    @Test
    public void testMatrixInPathAnnotationNotFound() {
        assureNotFound("/api-with-matrix;a");
    }

    @Test
    public void testMatrixInsteadOfPercentEncodedNotFound() {
        assureNotFound("/api-with-percent-encoded;a");
    }

    @Path("/api")
    @Authenticated
    public static class ApiResource {

        @GET
        public String api() {
            return "api";
        }

        @GET
        @Path("/service")
        public String service() {
            return "service";
        }
    }

    @Path("/api-with-matrix;a")
    public static class ApiWithMatrixResource {

        @GET
        public String apiWithMatrix() {
            throw new RuntimeException();
        }
    }

    @Path("/api-with-percent-encoded%3Ba")
    @Authenticated
    public static class ApiWithEncodedSemicolonResource {

        @GET
        public String apiWithEncodedSemicolon() {
            return "hello";
        }
    }

    private void assureAuthenticationFailure(String path) {
        assurePath(path, 401);
    }

    private void assureAuthenticationSuccess(String path) {
        assurePath(path, 200);
    }

    private void assureNotFound(String path) {
        assurePath(path, 404);
    }

    private void assurePath(String path, int expectedStatusCode) {
        var req = getClient().get(url.getPort(), url.getHost(), path);
        if (expectedStatusCode == 200) {
            req.basicAuthentication("admin", "admin");
        }
        var result = req.send();
        await().atMost(REQUEST_TIMEOUT).until(result::isComplete);
        assertEquals(expectedStatusCode, result.result().statusCode(), path);
    }
}
