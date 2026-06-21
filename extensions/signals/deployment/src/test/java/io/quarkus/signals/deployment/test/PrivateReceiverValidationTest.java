package io.quarkus.signals.deployment.test;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.Receives;
import io.quarkus.test.QuarkusExtensionTest;

public class PrivateReceiverValidationTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(PrivateReceiverBean.class))
            .setExpectedException(IllegalStateException.class);

    @Test
    public void testPrivateReceiverMethodFails() {
        // deployment should fail
    }

    @Singleton
    public static class PrivateReceiverBean {

        @SuppressWarnings("unused")
        private void onMsg(@Receives String msg) {
        }
    }

}
