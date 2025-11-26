package io.quarkus.smallrye.graphql.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.smallrye-graphql")
public interface SmallRyeGraphQLConfig {

    /**
     * The rootPath under which queries will be served. Default to graphql
     * By default, this value will be resolved as a path relative to `${quarkus.http.root-path}`.
     */
    @WithDefault("graphql")
    String rootPath();

    /**
     * Enable Apollo Federation. If this value is unspecified, then federation will be enabled
     * automatically if any GraphQL Federation annotations are detected in the application.
     */
    @WithName("federation.enabled")
    Optional<Boolean> federationEnabled();

    /**
     * Enable batch resolving for federation. Disabled by default.
     */
    @WithName("federation.batch-resolving-enabled")
    Optional<Boolean> federationBatchResolvingEnabled();

    /**
     * Enable metrics. By default, this is false. If set to true, a metrics extension is required.
     */
    @WithName("metrics.enabled")
    Optional<Boolean> metricsEnabled();

    /**
     * Enable tracing. By default, this will be enabled if the tracing extension is added.
     */
    @WithName("tracing.enabled")
    Optional<Boolean> tracingEnabled();

    /**
     * Enable eventing. Allow you to receive events on bootstrap and execution.
     */
    @WithName("events.enabled")
    @WithDefault("false")
    boolean eventsEnabled();

    /**
     * Enable non-blocking support. Default is true.
     */
    @WithName("nonblocking.enabled")
    Optional<Boolean> nonBlockingEnabled();

    /**
     * Change the type naming strategy.
     * All possible strategies are: default, merge-inner-class, full
     */
    @WithDefault("Default")
    String autoNameStrategy();

    /**
     * Print the data fetcher exception to the log file. Default `true` in dev and test mode, default `false` in prod.
     */
    Optional<Boolean> printDataFetcherException();

    /**
     * Make the schema available over HTTP.
     */
    @WithDefault("true")
    boolean schemaAvailable();

    /**
     * Subprotocols that should be supported by the server for graphql-over-websocket use cases.
     * Allowed subprotocols are "graphql-ws" and "graphql-transport-ws". By default, both are enabled.
     */
    Optional<List<String>> websocketSubprotocols();

    /**
     * SmallRye GraphQL UI configuration
     */
    @ConfigDocSection
    SmallRyeGraphQLUIConfig ui();

    /**
     * Additional scalars to register in the schema.
     * These are taken from the `graphql-java-extended-scalars` library.
     */
    Optional<List<ExtraScalar>> extraScalars();

    /**
     * The name of the key inside the client init payload which contains Authorization information.
     * The associated value of this is treated in the same way as sent over as a Authorization header.
     * Using headers is the preferred way, however in some languages, such as JavaScript, it is not possible to send
     * headers over websockets.
     * <br>
     * If sending headers are not an option, another viable option is
     * <a href=https://quarkus.io/guides/websockets-next-reference#bearer-token-authentication>Bearer Token Authentication</a>
     * which might be preferable over this in some cases as it's able to inject headers before the WebSocket is opened.
     * <br>
     * Default is undefined, which means the client init payload will not be checked for Authorization information.
     */
    Optional<String> authorizationClientInitPayloadName();
}
