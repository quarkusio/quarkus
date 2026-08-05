package io.quarkus.test.junit;

import java.io.Closeable;
import java.io.IOException;

import io.quarkus.test.common.TestResourceManager;

public class IntegrationTestExtensionState extends QuarkusTestExtensionState {

    public IntegrationTestExtensionState(TestResourceManager testResourceManager, Closeable resource, Runnable clearCallbacks) {
        super(testResourceManager, resource, clearCallbacks);
    }

    @Override
    protected void doClose() throws IOException {
        testResourceManager.close();
        resource.close();
    }
}
