package io.quarkus.test.junit;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.test.common.TestResourceManager;

public class IntegrationTestExtensionState implements ExtensionContext.Store.CloseableResource {

    private final TestResourceManager testResourceManager;
    private final Closeable resource;
    private final Map<String, String> sysPropRestore;
    private final Thread shutdownHook;

    IntegrationTestExtensionState(TestResourceManager testResourceManager, Closeable resource,
            Map<String, String> sysPropRestore) {
        this.testResourceManager = testResourceManager;
        this.resource = resource;
        this.sysPropRestore = sysPropRestore;
        this.shutdownHook = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    IntegrationTestExtensionState.this.close();
                } catch (IOException ignored) {
                }
            }
        }, "Quarkus Test Cleanup Shutdown task");
        Runtime.getRuntime().addShutdownHook(shutdownHook);

    }

    @Override
    public void close() throws IOException {
        testResourceManager.close();
        resource.close();
        for (Map.Entry<String, String> entry : sysPropRestore.entrySet()) {
            String val = entry.getValue();
            if (val == null) {
                System.clearProperty(entry.getKey());
            } else {
                System.setProperty(entry.getKey(), val);
            }
        }
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
    }

    public TestResourceManager getTestResourceManager() {
        return testResourceManager;
    }
}
