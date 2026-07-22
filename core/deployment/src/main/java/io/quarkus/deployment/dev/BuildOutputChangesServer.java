package io.quarkus.deployment.dev;

import java.io.IOException;

/**
 * Build-tool-side listener for externally produced dev-mode build output
 * batches.
 */
public interface BuildOutputChangesServer extends AutoCloseable {

    /**
     * Returns transport metadata to pass into the {@link DevModeContext} used to
     * launch Quarkus dev mode.
     */
    DevModeContext.ExternalBuildOutputTransport transport();

    /**
     * Sends one externally produced build-output batch to the connected Quarkus
     * dev-mode process and returns whether Quarkus applied it to the current
     * runtime state.
     */
    BuildOutputChangesApplyStatus send(BuildOutputChanges changes) throws IOException;

    @Override
    void close() throws IOException;
}
