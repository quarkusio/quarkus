package io.quarkus.websockets.next.test.security;

import static io.quarkus.websockets.next.test.security.SecurityTestBase.basicAuth;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.restassured.RestAssured;
import io.vertx.core.Vertx;

public class HttpUpgradeRedirectOnFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Endpoint.class, WSClient.class, TestIdentityProvider.class, TestIdentityController.class,
                            SecurityTestBase.class)
                    .addAsResource(
                            new StringAsset(
                                    "quarkus.websockets-next.server.security.auth-failure-redirect-url=https://quarkus.io\n"),
                            "application.properties"));

    @Inject
    Vertx vertx;

    @TestHTTPResource("end")
    URI endUri;

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", "admin")
                .add("user", "user", "user");
    }

    @Test
    public void testRedirectOnFailure() {
        // test redirected on failure
        RestAssured
                .given()
                // without this header the client would receive 404
                .header("Sec-WebSocket-Key", "foo")
                .redirects()
                .follow(false)
                .get(endUri)
                .then()
                .statusCode(302)
                .header(HttpHeaderNames.LOCATION.toString(), "https://quarkus.io");

        try (WSClient client = new WSClient(vertx)) {
            client.connect(basicAuth("admin", "admin"), endUri);
            client.waitForMessages(1);
            assertEquals("ready", client.getMessages().get(0).toString());
            client.sendAndAwait("hello");
            client.waitForMessages(2);
            assertEquals("hello", client.getMessages().get(1).toString());
        }

        // no redirect as CDI interceptor secures @OnTextMessage
        try (WSClient client = new WSClient(vertx)) {
            client.connect(basicAuth("user", "user"), endUri);
            client.waitForMessages(1);
            assertEquals("ready", client.getMessages().get(0).toString());
            client.sendAndAwait("hello");
            client.waitForMessages(2);
            assertEquals("forbidden:user", client.getMessages().get(1).toString());
        }
    }

    @RolesAllowed({ "admin", "user" })
    @WebSocket(path = "/end")
    public static class Endpoint {

        @Inject
        CurrentIdentityAssociation currentIdentity;

        @OnOpen
        String open() {
            return "ready";
        }

        @RolesAllowed("admin")
        @OnTextMessage
        String echo(String message) {
            if (!currentIdentity.getIdentity().hasRole("admin")) {
                throw new IllegalStateException();
            }
            return message;
        }

        @OnError
        String error(ForbiddenException t) {
            return "forbidden:" + currentIdentity.getIdentity().getPrincipal().getName();
        }

    }
}
