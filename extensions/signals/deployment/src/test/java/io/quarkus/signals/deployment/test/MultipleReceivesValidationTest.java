package io.quarkus.signals.deployment.test;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.Receives;
import io.quarkus.test.QuarkusExtensionTest;

public class MultipleReceivesValidationTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(MultipleReceivesBean.class))
            .setExpectedException(IllegalStateException.class);

    @Test
    public void testMultipleReceivesParamsFails() {
        // deployment should fail
    }

    @Singleton
    public static class MultipleReceivesBean {

        void onMsg(@Receives String first, @Receives String second) {
        }
    }
}
