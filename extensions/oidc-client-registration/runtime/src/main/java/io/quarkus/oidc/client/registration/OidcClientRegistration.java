package io.quarkus.oidc.client.registration;

import java.io.Closeable;
import java.util.List;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * OIDC client registration.
 */
public interface OidcClientRegistration extends Closeable {
    /**
     * Client registered at start-up with the configured metadata.
     *
     * @return {@link RegisteredClient}, null if no configured metadata is available.
     */
    Uni<RegisteredClient> registeredClient();

    /**
     * Register new client
     *
     * @param client client metadata for registering a new client
     * @return Uni<RegisteredClient>
     */
    Uni<RegisteredClient> registerClient(ClientMetadata client);

    /**
     * Register one or more new clients
     *
     * @param clients list of client metadata for registering new clients
     * @return Uni<RegisteredClient>
     */
    Multi<RegisteredClient> registerClients(List<ClientMetadata> clients);

    /**
     * Read an already registered client.
     *
     * @param registrationUri Address of the registration endpoint for the client.
     * @param registrationToken Registration token of the client
     * @return registered client.
     */
    Uni<RegisteredClient> readClient(String registrationUri, String registrationToken);
}
