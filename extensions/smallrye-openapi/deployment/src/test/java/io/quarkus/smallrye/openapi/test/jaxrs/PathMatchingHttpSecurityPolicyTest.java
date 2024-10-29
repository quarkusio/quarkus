package io.quarkus.smallrye.openapi.test.jaxrs;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.time.Duration;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

public class PathMatchingHttpSecurityPolicyTest {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final String APP_PROPS = """
            quarkus.http.auth.permission.management.paths=/q/*
            quarkus.http.auth.permission.management.policy=authenticated
            """;
    private static WebClient client;

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestIdentityController.class, TestIdentityProvider.class, PathHandler.class)
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties"));

    @BeforeAll
    public static void setup() {
        TestIdentityController.resetRoles()
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
            "/q/openapi", "///q/openapi", "/q///openapi", "/q/openapi/", "/q/openapi///"
    })
    public void testOpenApiPath(String path) {
        assurePath(path, 401);
        assurePathAuthenticated(path, "openapi");
    }

    private void assurePath(String path, int expectedStatusCode) {
        assurePath(path, expectedStatusCode, null, null, null);
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
}
