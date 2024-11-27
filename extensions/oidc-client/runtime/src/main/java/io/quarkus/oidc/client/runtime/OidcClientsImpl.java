package io.quarkus.oidc.client.runtime;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClientException;
import io.quarkus.oidc.client.OidcClients;
import io.smallrye.mutiny.Uni;

public class OidcClientsImpl implements OidcClients, Closeable {
    private OidcClient defaultClient;
    private Map<String, OidcClient> staticOidcClients;
    private Function<OidcClientConfig, Uni<OidcClient>> dynamicOidcClients;

    public OidcClientsImpl() {
    }

    public OidcClientsImpl(OidcClient defaultClient, Map<String, OidcClient> staticOidcClients,
            Function<OidcClientConfig, Uni<OidcClient>> dynamicOidcClients) {
        this.defaultClient = defaultClient;
        this.staticOidcClients = staticOidcClients;
        this.dynamicOidcClients = dynamicOidcClients;
    }

    @Override
    public OidcClient getClient() {
        return defaultClient;
    }

    @Override
    public OidcClient getClient(String id) {
        return staticOidcClients.get(id);
    }

    @Override
    public void close() throws IOException {
        defaultClient.close();
        for (OidcClient client : staticOidcClients.values()) {
            client.close();
        }
    }

    @Override
    public Uni<OidcClient> newClient(OidcClientConfig clientConfig) {
        if (clientConfig.id().isEmpty()) {
            throw new OidcClientException("'id' property must be set");
        }
        return dynamicOidcClients.apply(clientConfig);
    }

}
