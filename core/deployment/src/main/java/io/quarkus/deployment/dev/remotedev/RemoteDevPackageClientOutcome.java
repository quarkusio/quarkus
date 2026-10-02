package io.quarkus.deployment.dev.remotedev;

/**
 * Outcome of one {@link RemoteDevPackageClient} operation.
 *
 * <p>
 * <strong>API note:</strong>
 * These values describe the current Quarkus build-tool integration protocol. They are not a versioned
 * third-party protocol.
 */
public enum RemoteDevPackageClientOutcome {
    /**
     * A session was established. The accompanying result may request files needed to reconcile the endpoint.
     */
    CONNECTED,

    /**
     * A package difference was accepted by the current session.
     */
    SENT,

    /**
     * The current session is stale or restarted and its owner must connect a replacement client.
     */
    RECONNECT_REQUIRED
}
