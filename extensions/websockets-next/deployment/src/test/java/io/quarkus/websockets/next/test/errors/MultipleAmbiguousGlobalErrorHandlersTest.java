package io.quarkus.websockets.next.test.errors;

import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.WebSocketException;

public class MultipleAmbiguousGlobalErrorHandlersTest {

    @RegisterExtension
    public static final QuarkusExtensionTest test = new QuarkusExtensionTest()
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
        void onError1(IllegalStateException ise) {
        }

        @OnError
        void onError2(IllegalStateException ise) {
        }

    }

}
