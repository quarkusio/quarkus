package io.quarkus.test.junit.mockito.internal;

import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;

public class ResetMockQuarkusTestBeforeEachCallback implements QuarkusTestBeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context, boolean isSubstrateTest) {
        if (!isSubstrateTest) {
            MocksTracker.reset(context.getRequiredTestClass().getName());
        }
    }
}
