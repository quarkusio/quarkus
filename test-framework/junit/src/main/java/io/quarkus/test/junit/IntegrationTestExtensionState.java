package io.quarkus.test.junit;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

import io.quarkus.test.common.TestResourceManager;

public class IntegrationTestExtensionState extends QuarkusTestExtensionState {

    private final Path logPath;

    public IntegrationTestExtensionState(TestResourceManager testResourceManager,
            Closeable resource,
            Runnable clearCallbacks,
            Path logPath) {
        super(testResourceManager, resource, clearCallbacks);
        this.logPath = logPath;
    }

    @Override
    protected void doClose() throws IOException {
        testResourceManager.close();
        resource.close();
    }

    public Path getLogPath() {
        return logPath;
    }
}
