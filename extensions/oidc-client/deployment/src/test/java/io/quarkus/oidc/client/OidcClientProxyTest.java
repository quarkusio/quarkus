package io.quarkus.oidc.client;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jose4j.base64url.Base64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.vertx.core.Vertx;

@QuarkusTestResource(KeycloakRealmUserPasswordManager.class)
class OidcClientProxyTest {

    private static final Map<String, String> HEADERS = new ConcurrentHashMap<>();

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(OidcClientResource.class)
                    .addAsResource(new StringAsset("""
                            quarkus.keycloak.devservices.enabled=false
                            quarkus.oidc.enabled=false
                            quarkus.oidc-client.auth-server-url=${keycloak.url}/realms/quarkus/
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
                            """), "application.properties"));

    @Inject
    OidcClient oidcClient;

    @Inject
    Vertx vertx;

    @Test
    void assertProxyConfigurationAndOidcClientRecovery() throws InterruptedException, MalformedURLException {
        // the fact that this place is reached and the application startup did not fail also verifies that
        // when the OIDC client authentication server is not available on startup, we do not fail the build
        CountDownLatch latch = new CountDownLatch(1);
        var httpServer = vertx.createHttpServer();
        var httpClient = vertx.createHttpClient();
        var keycloakUrl = new URL(ConfigProvider.getConfig().getValue("keycloak.url", String.class));
        try {
            httpServer.requestHandler(serverReq -> {
                serverReq.headers().forEach(HEADERS::put);
                serverReq.body().onSuccess(serverReqBody -> httpClient
                        .request(serverReq.method(), keycloakUrl.getPort(), keycloakUrl.getHost(), serverReq.path())
                        .flatMap(clientReq -> {
                            serverReq.headers().forEach(clientReq::putHeader);
                            return clientReq.send(serverReqBody);
                        })
                        .onSuccess(clientResp -> clientResp.body().onSuccess(clientRespBody -> {
                            serverReq.response().setStatusCode(clientResp.statusCode());
                            clientResp.headers().forEach((k, v) -> serverReq.response().putHeader(k, v));
                            serverReq.response().end(clientRespBody);
                        }))
                        .onFailure(f -> serverReq.response().setStatusCode(500).end(f.getMessage())))
                        .onFailure(t -> serverReq.response().setStatusCode(500).end(t.getMessage()));
            });
            httpServer.listen(8765, "localhost").onSuccess(ignored -> latch.countDown());
            assertTrue(latch.await(15, TimeUnit.SECONDS), "Proxy server is not listening");

            // now that we have configured proxy, we expect that OIDC client can be used even though on startup
            // the OIDC metadata discovery has failed
            var tokens = oidcClient.getTokens().await().indefinitely();
            assertNotNull(tokens.getAccessToken());

            // assert proxy configuration
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
        } finally {
            httpClient.close();
            httpServer.close();
        }
    }
}
