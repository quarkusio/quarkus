package io.quarkus.smallrye.graphql.client.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class GraphQLClientConfig {

    /**
     * The URL location of the target GraphQL service.
     */
    @ConfigItem
    public Optional<String> url;

    /**
     * HTTP headers to add when communicating with the target GraphQL service.
     * Right now, this only works for the dynamic client.
     */
    @ConfigItem(name = "header")
    public Map<String, String> headers;

    /**
     * WebSocket subprotocols that should be supported by this client for running subscriptions over websockets.
     * Allowed values are:
     * - `graphql-ws` for the deprecated Apollo protocol
     * - `graphql-transport-ws` for the newer GraphQL over WebSocket protocol
     * If multiple protocols are provided, the actual protocol to be used will be subject to negotiation with
     * the server.
     * To make the client work with the dummy protocol implemented by SmallRye GraphQL 1.4 server-side,
     * leave this empty.
     */
    @ConfigItem
    public Optional<List<String>> subprotocols;

}
