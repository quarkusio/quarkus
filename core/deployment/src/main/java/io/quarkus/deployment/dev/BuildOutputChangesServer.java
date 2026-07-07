package io.quarkus.deployment.dev;

import java.io.IOException;
import java.util.concurrent.CompletionStage;

/**
 * Build-tool-side listener for externally produced dev-mode build output
 * batches.
 */
public interface BuildOutputChangesServer extends AutoCloseable {

    /**
     * Returns transport metadata to pass into the {@link DevModeContext} used to
     * launch Quarkus dev mode.
     *
     * @return loopback transport URI and per-server authentication token
     */
    DevModeContext.ExternalBuildOutputTransport transport();

    /**
     * Sends one externally produced build-output batch to the connected Quarkus
     * dev-mode process and returns whether Quarkus applied it to the current
     * runtime state.
     *
     * @param changes update to deliver
     * @return receiver apply status
     * @throws IOException when transport delivery fails
     */
    BuildOutputChangesApplyStatus send(BuildOutputChanges changes) throws IOException;

    /**
     * Completes normally after an explicit close and exceptionally when the
     * transport terminates unexpectedly.
     *
     * @return transport termination stage
     */
    CompletionStage<Void> termination();

    /**
     * Closes the listener and unblocks any pending transport operation.
     *
     * @throws IOException when transport cleanup fails
     */
    @Override
    void close() throws IOException;
}
