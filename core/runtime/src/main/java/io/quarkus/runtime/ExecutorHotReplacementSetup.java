package io.quarkus.runtime;

import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.dev.spi.HotReplacementSetup;

public class ExecutorHotReplacementSetup implements HotReplacementSetup {
    @Override
    public void setupHotDeployment(HotReplacementContext context) {

    }

    @Override
    public void close() {
        ExecutorRecorder.shutdownDevMode();
    }
}
