package io.quarkus.signals.deployment.test;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.Receives;
import io.quarkus.test.QuarkusExtensionTest;

public class StaticReceiverValidationTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(StaticReceiverBean.class))
            .setExpectedException(IllegalStateException.class);

    @Test
    public void testStaticReceiverMethodFails() {
        // deployment should fail
    }

    @Singleton
    public static class StaticReceiverBean {

        static void onMsg(@Receives String msg) {
        }
    }
}
