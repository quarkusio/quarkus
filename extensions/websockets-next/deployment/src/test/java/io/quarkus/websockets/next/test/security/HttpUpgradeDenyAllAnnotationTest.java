package io.quarkus.websockets.next.test.security;

import static io.quarkus.websockets.next.test.security.SecurityTestBase.basicAuth;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.concurrent.CompletionException;

import jakarta.annotation.security.DenyAll;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.util.ExceptionUtil;
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
import io.vertx.core.http.UpgradeRejectedException;

public class HttpUpgradeDenyAllAnnotationTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(Endpoint.class, AdminService.class, UserService.class,
                    TestIdentityProvider.class, TestIdentityController.class, WSClient.class, SecurityTestBase.class));

    @Inject
    Vertx vertx;

    @TestHTTPResource("end")
    URI endUri;

    @BeforeAll
    public static void setupUsers() {
        TestIdentityController.resetRoles().add("admin", "admin", "admin");
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
            CompletionException ce = assertThrows(CompletionException.class,
                    () -> client.connect(basicAuth("admin", "admin"), endUri));
            Throwable root = ExceptionUtil.getRootCause(ce);
            assertTrue(root instanceof UpgradeRejectedException);
            assertTrue(root.getMessage().contains("403"));
        }
    }

    @DenyAll
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
