package io.quarkus.deployment.dev.remotedev;

import java.io.IOException;
import java.util.Map;

/**
 * Synchronizes a locally built mutable application package with a Quarkus remote-development endpoint.
 * <p>
 * A build-tool session owns one client at a time. It first calls {@link #connect(Map)}, sends any files requested by
 * the endpoint with {@link #send(RemoteDevPackageDiff)}, and may then start stale-session polling with
 * {@link #startChangePolling(RemoteDevPackageReconnectListener)}. Subsequent package differences are sent through the
 * same client until the endpoint requires a reconnect or the owner closes the client.
 * <p>
 * Implementations may read changed files during {@code send}; callers must keep each captured file unchanged for the
 * duration of that call.
 *
 * <p>
 * <strong>API note:</strong>
 * This is an integration contract between aligned Quarkus build-tool and deployment components. It is not a
 * user-facing remote-development extension SPI, and compatibility across different Quarkus versions is not
 * guaranteed.
 */
public interface RemoteDevPackageClient extends AutoCloseable {

    /**
     * Establishes or re-establishes a remote-development session.
     *
     * @param localHashes non-{@code null} package-relative paths and their captured SHA-1 hashes
     * @return a non-{@code null} {@link RemoteDevPackageClientOutcome#CONNECTED} result containing any paths that the
     *         endpoint requires for initial reconciliation
     * @throws IOException if the endpoint cannot be contacted or rejects the connection
     */
    RemoteDevPackageClientResult connect(Map<String, String> localHashes) throws IOException;

    /**
     * Sends one package difference in the current session.
     *
     * @param diff the non-{@code null} difference to send
     * @return a non-{@code null} {@link RemoteDevPackageClientOutcome#SENT} result when the difference was accepted, or
     *         {@link RemoteDevPackageClientOutcome#RECONNECT_REQUIRED} when the caller must establish a new session
     * @throws IOException if the difference cannot be delivered
     */
    RemoteDevPackageClientResult send(RemoteDevPackageDiff diff) throws IOException;

    /**
     * Starts monitoring the connected endpoint for a stale session.
     * <p>
     * The client invokes the listener asynchronously when another session supersedes this one. Callers must complete
     * any initial reconciliation requested by {@link #connect(Map)} before starting polling.
     *
     * @param reconnectListener non-{@code null} listener that schedules a new build-tool iteration
     * @throws IOException if no fully reconciled session exists or polling cannot be started
     */
    void startChangePolling(RemoteDevPackageReconnectListener reconnectListener) throws IOException;

    /**
     * Stops background activity and releases resources owned by this client.
     *
     * @throws IOException if a resource or background worker cannot be stopped cleanly
     */
    @Override
    void close() throws IOException;
}
