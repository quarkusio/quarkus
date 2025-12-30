package io.quarkus.oidc.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jose4j.base64url.Base64;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;

@QuarkusTestResource(KeycloakRealmUserPasswordManager.class)
public class OidcClientProxyTest {

    private static final Map<String, String> HEADERS = new ConcurrentHashMap<>();
    private static volatile HttpServer httpServer;
    private static volatile Vertx vertx;

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(OidcClientResource.class)
                    .addAsResource(new StringAsset("""
                            quarkus.keycloak.devservices.enabled=false
                            quarkus.oidc.auth-server-url=${keycloak.url}/realms/quarkus/
                            quarkus.oidc-client.auth-server-url=${quarkus.oidc.auth-server-url}
                            quarkus.oidc-client.client-id=quarkus-app
                            quarkus.oidc-client.credentials.secret=secret
                            quarkus.oidc-client.grant.type=password
                            quarkus.oidc-client.grant-options.password.username=alice
                            quarkus.oidc-client.grant-options.password.password=alice
                            quarkus.oidc-client.proxy.proxy-configuration-name=oidc-proxy
                            quarkus.proxy.oidc-proxy.host=localhost
                            quarkus.proxy.oidc-proxy.port=8765
                            quarkus.proxy.oidc-proxy.username=name
                            quarkus.proxy.oidc-proxy.password=pwd
                            """), "application.properties"))
            .setBeforeAllCustomizer(() -> {
                CountDownLatch latch = new CountDownLatch(1);
                vertx = Vertx.vertx();
                httpServer = vertx.createHttpServer();
                httpServer.requestHandler(request -> {
                    request.headers().forEach(HEADERS::put);
                    request.response().end(" ");
                });
                httpServer.listen(8765, "localhost").onSuccess(ignored -> latch.countDown());
                try {
                    assertTrue(latch.await(15, TimeUnit.SECONDS), "Proxy server is not listening");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            })
            .assertException(throwable -> {
                // expect startup failure as our "proxy" is not redirecting requests
                var rootCause = ExceptionUtil.getRootCause(throwable);
                Assertions.assertNotNull(rootCause);
                Assertions.assertTrue(
                        rootCause.getMessage().toLowerCase().contains("OIDC Server is not available".toLowerCase()),
                        () -> "Expected exception message to contain 'OIDC Server is not available' but got: "
                                + rootCause.getMessage());

                // now assert proxy configuration
                Awaitility.await().atMost(10, TimeUnit.SECONDS)
                        .until(() -> HEADERS.containsKey("host") && HEADERS.containsKey("Proxy-Authorization"));
                assertTrue(HEADERS.get("host").contains("localhost:"),
                        () -> "Expected host header '%s' to contain 'localhost:'".formatted(HEADERS.get("host")));
                String proxyAuthorization = HEADERS.get("Proxy-Authorization");
                assertTrue(proxyAuthorization.contains("Basic "),
                        () -> "Proxy authorization does not contain basic authentication credentials: " + proxyAuthorization);
                String basicCredentials = new String(Base64.decode(proxyAuthorization.substring("Basic ".length()).trim()),
                        StandardCharsets.UTF_8);
                Assertions.assertEquals("name:pwd", basicCredentials);
            });

    @Test
    void runTest() {
        Assertions.fail("Application startup should had failed due to unreachable authentication server");
    }

    @AfterAll
    static void cleanResources() {
        if (httpServer != null) {
            httpServer.close();
        }
        if (vertx != null) {
            vertx.close();
        }
    }
}
