package io.quarkus.deployment.dev.remotedev;

import java.io.IOException;

/**
 * Creates the package client owned by one build-tool remote-development session.
 *
 * <p>
 * <strong>API note:</strong>
 * This factory is a Quarkus build-tool integration seam, including for tests. It is not a general-purpose
 * client-provider SPI and has no compatibility guarantee across different Quarkus versions.
 */
public interface RemoteDevPackageClientFactory {

    /**
     * Creates a new client. The caller owns the returned client and is responsible for closing it.
     *
     * @param config non-{@code null} connection configuration
     * @return a non-{@code null}, not-yet-connected client
     * @throws IOException if the client cannot be initialized
     */
    RemoteDevPackageClient create(RemoteDevPackageClientConfig config) throws IOException;
}
