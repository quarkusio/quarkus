package org.jboss.resteasy.reactive.client.api;

public class QuarkusRestClientProperties {

    /**
     * Configure the connect timeout in ms.
     */
    public static final String CONNECT_TIMEOUT = "io.quarkus.rest.client.connect-timeout";
    /**
     * maximum number of redirects for a client call. Works only if the client has `followingRedirects enabled
     */
    public static final String MAX_REDIRECTS = "io.quarkus.rest.client.max-redirects";

    public static final String READ_TIMEOUT = "io.quarkus.rest.client.read-timeout";

    /**
     * See {@link io.netty.handler.codec.http.multipart.HttpPostRequestEncoder.EncoderMode}, RFC1738 by default
     */
    public static final String MULTIPART_ENCODER_MODE = "io.quarkus.rest.client.multipart-post-encoder-mode";

    /**
     * How long should an open connection stay in the rest client connection pool. Value must be in seconds.
     */
    public static final String CONNECTION_TTL = "io.quarkus.rest.client.connection-ttl";

    /**
     * The size of the rest client connection pool.
     */
    public static final String CONNECTION_POOL_SIZE = "io.quarkus.rest.client.connection-pool-size";

    public static final String STATIC_HEADERS = "io.quarkus.rest.client.static-headers";

    /**
     * Set to true to share the HTTP client between REST clients.
     * There can be multiple shared clients distinguished by <em>name</em>, when no specific name is set,
     * the name <code>__vertx.DEFAULT</code> is used.
     */
    public static final String SHARED = "io.quarkus.rest.client.shared";

    /**
     * Set the HTTP client name, used when the client is shared, otherwise ignored.
     */
    public static final String NAME = "io.quarkus.rest.client.name";

    /**
     * Set to true to prevent the client from providing additional contextual information (REST client class and method names)
     * when exception happens during a client invocation.
     */
    public static final String DISABLE_CONTEXTUAL_ERROR_MESSAGES = "io.quarkus.rest.client.disable-contextual-error-messages";

    public static final String USER_AGENT = "io.quarkus.rest.client.user-agent";

}
