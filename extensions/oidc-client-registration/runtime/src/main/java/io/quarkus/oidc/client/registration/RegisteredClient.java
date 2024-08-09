package io.quarkus.oidc.client.registration;

import java.io.Closeable;

import io.smallrye.mutiny.Uni;

/**
 * Client registered with {@link OidcClientConfiguration}
 */
public interface RegisteredClient extends Closeable {

    /**
     * Return current metadata of the registered client.
     *
     * @return Metadata of the registered client.
     */
    ClientMetadata metadata();

    /**
     * Read current metadata of the registered client from this client's registration endpoint.
     *
     * @return Registered client containing current metadata.
     */
    Uni<RegisteredClient> read();

    /**
     * Update metadata of the registered client using this client's registration endpoint.
     *
     * @return Registered client containing updated metadata.
     */
    Uni<RegisteredClient> update(ClientMetadata metadata);

    /**
     * Delete registered client from this client's registration endpoint.
     */
    Uni<Void> delete();
}
