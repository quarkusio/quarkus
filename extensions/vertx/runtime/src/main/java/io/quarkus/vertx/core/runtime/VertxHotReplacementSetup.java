package io.quarkus.vertx.core.runtime;

import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.dev.spi.HotReplacementSetup;

public class VertxHotReplacementSetup implements HotReplacementSetup {
    @Override
    public void setupHotDeployment(HotReplacementContext context) {

    }

    @Override
    public void close() {
        VertxCoreRecorder.shutdownDevMode();
    }
}
