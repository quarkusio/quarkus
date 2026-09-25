package io.quarkus.deployment.dev;

import java.io.IOException;

/**
 * Quarkus-side connection to an external build-output producer.
 */
interface BuildOutputChangesConnection extends AutoCloseable {

    void liveReloadStateChanged(BuildOutputLiveReloadState state);

    @Override
    void close() throws IOException;
}
