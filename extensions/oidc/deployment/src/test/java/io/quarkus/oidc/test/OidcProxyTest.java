package io.quarkus.oidc.test;

import static io.quarkus.oidc.runtime.OidcUtils.TENANT_ID_ATTRIBUTE;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.dd.plist.Base64;

import io.quarkus.security.Authenticated;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

@QuarkusTestResource(KeycloakTestResourceLifecycleManager.class)
class OidcProxyTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BearerResource.class)
                    .addAsResource(
                            new StringAsset(
                                    """
                                            quarkus.keycloak.devservices.enabled=false
                                            quarkus.oidc.auth-server-url=${keycloak.url}/realms/quarkus
                                            quarkus.oidc.client-id=quarkus-service-app
                                            quarkus.oidc.credentials.secret=secret
                                            quarkus.oidc.proxy.proxy-configuration-name=oidc-proxy
                                            quarkus.proxy.oidc-proxy.host=localhost
                                            quarkus.proxy.oidc-proxy.port=8765
                                            quarkus.proxy.oidc-proxy.username=name
                                            quarkus.proxy.oidc-proxy.password=pwd
                                            """),
                            "application.properties"));

    @Inject
    Vertx vertx;

    @Test
    void testProxyConfigApplied() throws InterruptedException, IOException {
        CountDownLatch latch = new CountDownLatch(1);
        var httpServer = vertx.createHttpServer();
        var headers = new ConcurrentHashMap<String, String>();
        try {
            httpServer.requestHandler(request -> {
                request.headers().forEach(headers::put);
                request.response().end(" ");
            });
            httpServer.listen(8765, "localhost").onSuccess(ignored -> latch.countDown());
            assertTrue(latch.await(15, TimeUnit.SECONDS), "Proxy server is not listening");
            String authServerUrl = ConfigProvider.getConfig().getValue("quarkus.oidc.auth-server-url", String.class);
            callSecuredEndpoint(authServerUrl);
            Awaitility.await().atMost(10, TimeUnit.SECONDS)
                    .until(() -> headers.containsKey("host") && headers.containsKey("Proxy-Authorization"));
            assertTrue(authServerUrl.contains(headers.get("host")),
                    () -> "Expected Keycloak URL '%s' to contain host header '%s'".formatted(authServerUrl,
                            headers.get("host")));
            String proxyAuthorization = headers.get("Proxy-Authorization");
            assertTrue(proxyAuthorization.contains("Basic "),
                    () -> "Proxy authorization does not contain basic authentication credentials: " + proxyAuthorization);
            String basicCredentials = new String(Base64.decode(proxyAuthorization.substring("Basic ".length()).trim()),
                    StandardCharsets.UTF_8);
            Assertions.assertEquals("name:pwd", basicCredentials);
        } finally {
            httpServer.close();
        }
    }

    private static void callSecuredEndpoint(String authServerUrl) {
        var token = new KeycloakTestClient(authServerUrl).getAccessToken("alice");
        RestAssured.given()
                .contentType(ContentType.TEXT)
                .accept(ContentType.TEXT)
                .auth().oauth2(token)
                .get("/bearer/tenant")
                .then().statusCode(500);
    }

    @Path("bearer")
    public static class BearerResource {

        @Inject
        RoutingContext routingContext;

        @Authenticated
        @GET
        @Path("tenant")
        public String getTenant() {
            return routingContext.get(TENANT_ID_ATTRIBUTE);
        }

    }

}
