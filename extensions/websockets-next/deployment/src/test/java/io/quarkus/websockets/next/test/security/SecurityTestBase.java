package io.quarkus.websockets.next.test.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.CompletionException;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.util.ExceptionUtil;
import io.quarkus.security.StringPermission;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.UpgradeRejectedException;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;

public abstract class SecurityTestBase {

    @Inject
    Vertx vertx;

    @TestHTTPResource("end")
    URI endUri;

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles()
                .add("admin", "admin", new StringPermission("endpoint", "read"), new StringPermission("perm1"))
                .add("almighty", "almighty", new StringPermission("perm1"), new StringPermission("perm2"))
                .add("user", "user", new StringPermission("endpoint", "connect"), new StringPermission("perm2"));
    }

    @Test
    public void testEndpoint() {
        try (WSClient client = new WSClient(vertx)) {
            CompletionException ce = assertThrows(CompletionException.class, () -> client.connect(endUri));
            Throwable root = ExceptionUtil.getRootCause(ce);
            assertTrue(root instanceof UpgradeRejectedException);
            assertTrue(root.getMessage().contains("401"));
        }
        try (WSClient client = new WSClient(vertx)) {
            client.connect(basicAuth("admin", "admin"), endUri);
            client.waitForMessages(1);
            assertEquals("ready", client.getMessages().get(0).toString());
            client.sendAndAwait("hello");
            client.waitForMessages(2);
            assertEquals("hello", client.getMessages().get(1).toString());
        }
        try (WSClient client = new WSClient(vertx)) {
            client.connect(basicAuth("user", "user"), endUri);
            client.waitForMessages(1);
            assertEquals("ready", client.getMessages().get(0).toString());
            client.sendAndAwait("hello");
            client.waitForMessages(2);
            assertEquals("forbidden:user", client.getMessages().get(1).toString());
        }
    }

    static WebSocketConnectOptions basicAuth(String username, String password) {
        return new WebSocketConnectOptions().addHeader(HttpHeaders.AUTHORIZATION.toString(),
                new UsernamePasswordCredentials(username, password).applyHttpChallenge(null).toHttpAuthorization());
    }

}
