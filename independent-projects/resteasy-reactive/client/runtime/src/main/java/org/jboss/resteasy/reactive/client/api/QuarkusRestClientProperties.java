package org.jboss.resteasy.reactive.client.api;

import org.jboss.resteasy.reactive.client.impl.multipart.PausableHttpPostRequestEncoder;

public class QuarkusRestClientProperties {

    /**
     * Configures the maximum chunk size.
     */
    public static final String MAX_CHUNK_SIZE = "io.quarkus.rest.client.max-chunk-size";

    /**
     * Configure the connect timeout in ms.
     */
    public static final String CONNECT_TIMEOUT = "io.quarkus.rest.client.connect-timeout";
    /**
     * maximum number of redirects for a client call. Works only if the client has `followingRedirects enabled
     */
    public static final String MAX_REDIRECTS = "io.quarkus.rest.client.max-redirects";
    /**
     * maximum length of all headers for HTTP/1.x.
     */
    public static final String MAX_HEADER_SIZE = "io.quarkus.rest.client.max-header-size";
    /**
     * maximum length of the initial line for HTTP/1.x.
     */
    public static final String MAX_INITIAL_LINE_LENGTH = "io.quarkus.rest.client.max-initial-line-length";

    public static final String READ_TIMEOUT = "io.quarkus.rest.client.read-timeout";

    /**
     * See {@link PausableHttpPostRequestEncoder.EncoderMode}, RFC1738 by default
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

    /**
     * A boolean value used to determine whether the keep alive is enabled or disabled.
     */
    public static final String KEEP_ALIVE_ENABLED = "io.quarkus.rest.client.keep-alive-enabled";

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

    /**
     * Set to true to explicitly use the HTTP/2 version.
     */
    public static final String HTTP2 = "io.quarkus.rest.client.http2";

    /**
     * Configures the HTTP/2 upgrade maximum length of the aggregated content in bytes.
     */
    public static final String HTTP2_UPGRADE_MAX_CONTENT_LENGTH = "io.quarkus.rest.client.http2UpgradeMaxContentLength";

    /**
     * Set to true to explicitly use the Application-Layer Protocol Negotiation extension.
     */
    public static final String ALPN = "io.quarkus.rest.client.alpn";

    /**
     * If set to true, the stacktrace of the invocation of the REST Client method is captured
     */
    public static final String CAPTURE_STACKTRACE = "io.quarkus.rest.client.capture-stacktrace";

    /**
     * Scope of logging for the client.
     * <br/>
     * WARNING: beware of logging sensitive data
     * <br/>
     * The possible values are:
     * <ul>
     * <li>{@code request-response} - enables logging request and responses, including redirect responses</li>
     * <li>{@code all} - enables logging requests and responses and lower-level logging</li>
     * <li>{@code none} - no additional logging</li>
     * </ul>
     *
     * This property is applicable to reactive REST clients only.
     */
    public static final String LOGGING_SCOPE = "io.quarkus.rest.client.logging.scope";

    /**
     * How many characters of the body should be logged. Message body can be large and can easily pollute the logs.
     * <p>
     * By default, set to 100.
     * <p>
     * This property is applicable to reactive REST clients only.
     */
    public static final String LOGGING_BODY_LIMIT = "io.quarkus.rest.client.logging.body-limit";

}
