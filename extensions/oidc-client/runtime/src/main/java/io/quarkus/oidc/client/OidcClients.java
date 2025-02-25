package io.quarkus.oidc.client;

import java.io.Closeable;

import io.quarkus.oidc.client.runtime.OidcClientConfig;
import io.smallrye.mutiny.Uni;

/**
 * Token grant clients
 */
public interface OidcClients extends Closeable {

    /**
     * Returns a default {@link OidcClient}.
     *
     * @return {@link OidcClient}
     */
    OidcClient getClient();

    /**
     * Returns an {@link OidcClient} with a specific id.
     *
     * @param id {@link OidcClient} id
     * @return {@link OidcClient}
     */
    OidcClient getClient(String id);

    /**
     * Returns a new {@link OidcClient}.
     *
     * @param clientConfig {@link OidcClientConfig} new client configuration
     * @return Uni<OidcClient>
     */
    Uni<OidcClient> newClient(OidcClientConfig clientConfig);
}
