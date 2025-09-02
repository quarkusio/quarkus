package io.quarkus.smallrye.graphql.client.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigGroup
public interface GraphQLClientConfig {

    /**
     * The URL location of the target GraphQL service.
     */
    Optional<String> url();

    /**
     * HTTP headers to add when communicating with the target GraphQL service.
     */
    @WithName("header")
    @ConfigDocMapKey("header-name")
    public Map<String, String> headers();

    /**
     * WebSocket subprotocols that should be supported by this client for running GraphQL operations over websockets.
     * Allowed values are:
     * - `graphql-ws` for the deprecated Apollo protocol
     * - `graphql-transport-ws` for the newer GraphQL over WebSocket protocol (default value)
     * If multiple protocols are provided, the actual protocol to be used will be subject to negotiation with
     * the server.
     */
    @WithDefault("graphql-transport-ws")
    Optional<List<String>> subprotocols();

    /**
     * If true, then queries and mutations will run over the websocket transport rather than pure HTTP.
     * Off by default, because it has higher overhead.
     */
    Optional<Boolean> executeSingleResultOperationsOverWebsocket();

    /**
     * Maximum time in milliseconds that will be allowed to wait for the server to acknowledge a websocket connection
     * (send a subprotocol-specific ACK message).
     */
    OptionalInt websocketInitializationTimeout();

    /**
     * Hostname of the proxy to use.
     */
    Optional<String> proxyHost();

    /**
     * Port number of the proxy to use.
     */
    OptionalInt proxyPort();

    /**
     * Username for the proxy to use.
     */
    Optional<String> proxyUsername();

    /**
     * Password for the proxy to use.
     */
    Optional<String> proxyPassword();

    /**
     * Maximum number of redirects to follow.
     */
    OptionalInt maxRedirects();

    /**
     * Additional payload sent on websocket initialization.
     */
    @WithName("init-payload")
    @ConfigDocMapKey("property-name")
    Map<String, String> initPayload();

    /**
     * Allowing unexpected fields in response.
     * If true, there will be warning log of an unexpected field.
     * Else it throws an error.
     */
    Optional<Boolean> allowUnexpectedResponseFields();

    /**
     * The name of the TLS configuration (bucket) used for client authentication in the TLS registry.
     */
    Optional<String> tlsConfigurationName();
}
