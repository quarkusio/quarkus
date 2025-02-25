package io.quarkus.websockets.next.test.client.programmatic;

import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.websockets.next.WebSocketClientException;
import io.quarkus.websockets.next.WebSocketConnector;

public class InvalidConnectorProgrammaticInjectionPointTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Service.class);
            })
            .setExpectedException(WebSocketClientException.class, true);

    @Test
    void testInvalidInjectionPoint() {
        fail();
    }

    @Unremovable
    @Singleton
    public static class Service {

        @Inject
        Instance<WebSocketConnector<String>> invalid;

    }

}
