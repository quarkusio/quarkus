package io.quarkus.websockets.next.test.security;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.ApplicationScoped;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.test.utils.WSClient;

public class HttpUpgradePermissionCheckerArgsValidationFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Endpoint.class, WSClient.class, TestIdentityProvider.class, TestIdentityController.class,
                            Checker.class))
            .assertException(t -> {
                assertInstanceOf(IllegalArgumentException.class, t);
                assertTrue(t.getMessage()
                        .contains("@PermissionAllowed instance that accepts method arguments must be placed on a method"));
            });

    @Test
    public void test() {
        Assertions.fail();
    }

    @PermissionsAllowed("echo")
    @WebSocket(path = "/endpoint")
    public static class Endpoint {

        @OnTextMessage
        String echo(String message) {
            return message;
        }

    }

    @ApplicationScoped
    public static class Checker {

        @PermissionChecker("echo")
        boolean canEcho(String message) {
            return true;
        }

    }

}
