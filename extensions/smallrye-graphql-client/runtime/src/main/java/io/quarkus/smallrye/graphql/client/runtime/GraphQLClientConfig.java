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
     * The name of the proxy configuration to use.
     *
     * If not set and the default proxy configuration is configured ({@code quarkus.proxy.*}) then that will be used.
     * If the proxy configuration name is set, the configuration from {@code quarkus.proxy.<name>.*} will be used.
     * If the proxy configuration name is set, but no proxy configuration is found with that name, then an error will be thrown
     * at runtime.
     */
    Optional<String> proxyConfigurationName();

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
