package io.quarkus.websockets.next.test.errors;

import static org.junit.jupiter.api.Assertions.fail;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.WebSocketException;

public class MultipleAmbiguousGlobalErrorHandlersTest {

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

    @Unremovable
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
