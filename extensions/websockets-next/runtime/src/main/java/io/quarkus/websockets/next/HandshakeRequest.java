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
     * @see HandshakeRequest#SEC_WEBSOCKET_KEY
     * @see HandshakeRequest#SEC_WEBSOCKET_ACCEPT
     * @see HandshakeRequest#SEC_WEBSOCKET_EXTENSIONS
     * @see HandshakeRequest#SEC_WEBSOCKET_PROTOCOL
     * @see HandshakeRequest#SEC_WEBSOCKET_VERSION
     */
    String header(String name);

    /**
     * The name is case insensitive.
     *
     * @param name
     * @return an immutable list of header values for the given header name, never {@code null}
     * @see HandshakeRequest#SEC_WEBSOCKET_KEY
     * @see HandshakeRequest#SEC_WEBSOCKET_ACCEPT
     * @see HandshakeRequest#SEC_WEBSOCKET_EXTENSIONS
     * @see HandshakeRequest#SEC_WEBSOCKET_PROTOCOL
     * @see HandshakeRequest#SEC_WEBSOCKET_VERSION
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
     * @return the scheme component of the server endpoint URL
     */
    String scheme();

    /**
     *
     * @return the host component of the server endpoint URL
     */
    String host();

    /**
     *
     * @return the port number of the server endpoint URL
     */
    int port();

    /**
     *
     * @return the path component of the server endpoint URL
     */
    String path();

    /**
     * @return the query string of the server endpoint URL
     */
    String query();

    /**
     *
     * @return the local IP address and port for this connection, or {@code null} if not available
     */
    String localAddress();

    /**
     * @return the remote IP address and port for this connection, or {@code null} if not available
     */
    String remoteAddress();

    /**
     * See <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-11.3.1">Sec-WebSocket-Key</a>.
     */
    String SEC_WEBSOCKET_KEY = "Sec-WebSocket-Key";

    /**
     * See <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-11.3.2">Sec-WebSocket-Extensions</a>.
     */
    String SEC_WEBSOCKET_EXTENSIONS = "Sec-WebSocket-Extensions";

    /**
     * See <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-11.3.3">Sec-WebSocket-Accept</a>.
     */
    String SEC_WEBSOCKET_ACCEPT = "Sec-WebSocket-Accept";

    /**
     * See <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-11.3.4">Sec-WebSocket-Protocol</a>.
     */
    String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";

    /**
     * See <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-11.3.5">Sec-WebSocket-Version</a>.
     */
    String SEC_WEBSOCKET_VERSION = "Sec-WebSocket-Version";

}
