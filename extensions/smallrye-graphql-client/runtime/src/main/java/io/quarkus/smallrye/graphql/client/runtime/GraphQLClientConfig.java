package io.quarkus.smallrye.graphql.client.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
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
     */
    @ConfigItem(name = "header")
    @ConfigDocMapKey("header-name")
    public Map<String, String> headers;

    /**
     * WebSocket subprotocols that should be supported by this client for running GraphQL operations over websockets.
     * Allowed values are:
     * - `graphql-ws` for the deprecated Apollo protocol
     * - `graphql-transport-ws` for the newer GraphQL over WebSocket protocol (default value)
     * If multiple protocols are provided, the actual protocol to be used will be subject to negotiation with
     * the server.
     */
    @ConfigItem(defaultValue = "graphql-transport-ws")
    public Optional<List<String>> subprotocols;

    /**
     * If true, then queries and mutations will run over the websocket transport rather than pure HTTP.
     * Off by default, because it has higher overhead.
     */
    @ConfigItem
    public Optional<Boolean> executeSingleResultOperationsOverWebsocket;

    /**
     * Maximum time in milliseconds that will be allowed to wait for the server to acknowledge a websocket connection
     * (send a subprotocol-specific ACK message).
     */
    @ConfigItem
    public OptionalInt websocketInitializationTimeout;

    /**
     * The trust store location. Can point to either a classpath resource or a file.
     *
     * @deprecated This configuration property is deprecated. Consider using the Quarkus TLS registry.
     *             Set the desired TLS bucket name using the following configuration property:
     *             {@code quarkus.smallrye-graphql-client."client-name".tls-bucket-name}.
     */
    @ConfigItem
    @Deprecated(forRemoval = true, since = "3.16.0")
    public Optional<String> trustStore;

    /**
     * The trust store password.
     *
     * @deprecated This configuration property is deprecated. Consider using the Quarkus TLS registry.
     *             Set the desired TLS bucket name using the following configuration property:
     *             {@code quarkus.smallrye-graphql-client."client-name".tls-bucket-name}.
     */
    @ConfigItem
    @Deprecated(forRemoval = true, since = "3.16.0")
    public Optional<String> trustStorePassword;

    /**
     * The type of the trust store. Defaults to "JKS".
     *
     * @deprecated This configuration property is deprecated. Consider using the Quarkus TLS registry.
     *             Set the desired TLS bucket name using the following configuration property:
     *             {@code quarkus.smallrye-graphql-client."client-name".tls-bucket-name}.
     */
    @ConfigItem
    @Deprecated(forRemoval = true, since = "3.16.0")
    public Optional<String> trustStoreType;

    /**
     * The key store location. Can point to either a classpath resource or a file.
     *
     * @deprecated This configuration property is deprecated. Consider using the Quarkus TLS registry.
     *             Set the desired TLS bucket name using the following configuration property:
     *             {@code quarkus.smallrye-graphql-client."client-name".tls-bucket-name}.
     */
    @ConfigItem
    @Deprecated(forRemoval = true, since = "3.16.0")
    public Optional<String> keyStore;

    /**
     * The key store password.
     *
     * @deprecated This configuration property is deprecated. Consider using the Quarkus TLS registry.
     *             Set the desired TLS bucket name using the following configuration property:
     *             {@code quarkus.smallrye-graphql-client."client-name".tls-bucket-name}.
     */
    @ConfigItem
    @Deprecated(forRemoval = true, since = "3.16.0")
    public Optional<String> keyStorePassword;

    /**
     * The type of the key store. Defaults to "JKS".
     *
     * @deprecated This configuration property is deprecated. Consider using the Quarkus TLS registry.
     *             Set the desired TLS bucket name using the following configuration property:
     *             {@code quarkus.smallrye-graphql-client."client-name".tls-bucket-name}.
     *
     */
    @ConfigItem
    @Deprecated(forRemoval = true, since = "3.16.0")
    public Optional<String> keyStoreType;

    /**
     * Hostname of the proxy to use.
     */
    @ConfigItem
    public Optional<String> proxyHost;

    /**
     * Port number of the proxy to use.
     */
    @ConfigItem
    public OptionalInt proxyPort;

    /**
     * Username for the proxy to use.
     */
    @ConfigItem
    public Optional<String> proxyUsername;

    /**
     * Password for the proxy to use.
     */
    @ConfigItem
    public Optional<String> proxyPassword;

    /**
     * Maximum number of redirects to follow.
     */
    @ConfigItem
    public OptionalInt maxRedirects;

    /**
     * Additional payload sent on websocket initialization.
     */
    @ConfigItem(name = "init-payload")
    @ConfigDocMapKey("property-name")
    public Map<String, String> initPayload;

    /**
     * Allowing unexpected fields in response.
     * If true, there will be warning log of an unexpected field.
     * Else it throws an error.
     */
    @ConfigItem
    public Optional<Boolean> allowUnexpectedResponseFields;

    /**
     * The name of the TLS configuration (bucket) used for client authentication in the TLS registry.
     */
    @ConfigItem
    public Optional<String> tlsConfigurationName;
}
