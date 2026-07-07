package io.quarkus.deployment.dev.remotedev;

import java.io.IOException;

/**
 * Receives an asynchronous notification that the current remote-development session must reconnect.
 * <p>
 * Implementations should schedule the owning build-tool session's reconnect iteration and return promptly. The
 * built-in HTTP client may retry the notification when this callback reports a failure.
 *
 * <p>
 * <strong>API note:</strong>
 * This callback belongs to the Quarkus build-tool remote-development integration contract and is not an
 * application callback API.
 */
@FunctionalInterface
public interface RemoteDevPackageReconnectListener {

    /**
     * Requests a reconnect iteration.
     *
     * @throws IOException if the reconnect cannot be scheduled
     */
    void reconnectRequired() throws IOException;
}
