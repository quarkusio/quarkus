package io.quarkus.websockets.next.test.errors;

import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.PathParam;
import io.quarkus.websockets.next.WebSocketException;

public class GlobalErrorHandlerWithPathParamTest {

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(GlobalErrorHandlers.class);
            })
            .setExpectedException(WebSocketException.class);

    @Test
    void testMultipleAmbiguousErrorHandlers() {
        fail();
    }

    @ApplicationScoped
    public static class GlobalErrorHandlers {

        @OnError
        void onError(IllegalStateException ise, @PathParam String illegal) {
        }

    }

}
