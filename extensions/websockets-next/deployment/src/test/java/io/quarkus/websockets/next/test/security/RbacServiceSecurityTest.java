package io.quarkus.websockets.next.test.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.Set;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;
import io.vertx.core.Vertx;

public class RbacServiceSecurityTest extends SecurityTestBase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(Endpoint.class, AdminService.class, UserService.class,
                    TestIdentityProvider.class, TestIdentityController.class, WSClient.class));

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
    public void testEndpoint() {
        try (WSClient client = new WSClient(vertx)) {
            client.connect(basicAuth("admin", "admin"), endUri);
            client.sendAndAwait("hello"); // admin service
            client.sendAndAwait("hi"); // forbidden
            client.waitForMessages(2);
            assertEquals(Set.of("24", "forbidden"), Set.copyOf(client.getMessages().stream().map(Object::toString).toList()));
        }
        try (WSClient client = new WSClient(vertx)) {
            client.connect(basicAuth("user", "user"), endUri);
            client.sendAndAwait("hello"); // forbidden
            client.sendAndAwait("hi"); // user service
            client.waitForMessages(2);
            assertEquals(Set.of("42", "forbidden"), Set.copyOf(client.getMessages().stream().map(Object::toString).toList()));
        }
    }

    @WebSocket(path = "/end")
    public static class Endpoint {

        @Inject
        UserService userService;

        @Inject
        AdminService adminService;

        @OnTextMessage
        String echo(String message) {
            return message.equals("hello") ? adminService.ping() : userService.ping();
        }

        @OnError
        String error(ForbiddenException t) {
            return "forbidden";
        }

    }

}
