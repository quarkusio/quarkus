package io.quarkus.websockets.next;

import java.util.List;
import java.util.Map;

/**
 * Provides some useful information about the initial handshake request.
 */
public interface HandshakeRequest {

    /**
     * The name is case insensitive.
     *
     * @param name
     * @return the first header value for the given header name, or {@code null}
     */
    String header(String name);

    /**
     * The name is case insensitive.
     *
     * @param name
     * @return an immutable list of header values for the given header name, never {@code null}
     */
    List<String> headers(String name);

    /**
     * Returned header names are lower case.
     *
     * @return an immutable map of header names to header values
     */
    Map<String, List<String>> headers();

    /**
     *
     * @return the scheme
     */
    String scheme();

    /**
     *
     * @return the host
     */
    String host();

    /**
     *
     * @return the port
     */
    int port();

    /**
     *
     * @return the path
     */
    String path();

    /**
     *
     * @return the query string
     */
    String query();

    /**
     * See <a href="https://datatracker.ietf.org/doc/html/rfc6455#page-57">The WebSocket Protocol</a>.
     */
    public static final String SEC_WEBSOCKET_KEY = "Sec-WebSocket-Key";

    /**
     * See <a href="https://datatracker.ietf.org/doc/html/rfc6455#page-58">The WebSocket Protocol</a>.
     */
    public static final String SEC_WEBSOCKET_EXTENSIONS = "Sec-WebSocket-Extensions";

    /**
     * See <a href="https://datatracker.ietf.org/doc/html/rfc6455#page-58">The WebSocket Protocol</a>.
     */
    public static final String SEC_WEBSOCKET_ACCEPT = "Sec-WebSocket-Accept";

    /**
     * See <a href="https://datatracker.ietf.org/doc/html/rfc6455#page-59">The WebSocket Protocol</a>.
     */
    public static final String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";

    /**
     * See <a href="https://datatracker.ietf.org/doc/html/rfc6455#page-60">The WebSocket Protocol</a>.
     */
    public static final String SEC_WEBSOCKET_VERSION = "Sec-WebSocket-Version";

}