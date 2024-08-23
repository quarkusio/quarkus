package io.quarkus.websockets.next;

import java.net.URI;
import java.net.URLEncoder;

import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.common.annotation.Experimental;
import io.smallrye.mutiny.Uni;

/**
 * This connector can be used to configure and open new client connections using a client endpoint class.
 * <p>
 * This construct is not thread-safe and should not be used concurrently.
 *
 * @param <CLIENT> The client endpoint class
 * @see WebSocketClient
 * @see WebSocketClientConnection
 */
@Experimental("This API is experimental and may change in the future")
public interface WebSocketConnector<CLIENT> {

    /**
     * Set the base URI.
     *
     * @param baseUri
     * @return self
     */
    WebSocketConnector<CLIENT> baseUri(URI baseUri);

    /**
     * Set the base URI.
     *
     * @param baseUri
     * @return self
     */
    default WebSocketConnector<CLIENT> baseUri(String baseUri) {
        return baseUri(URI.create(baseUri));
    }

    /**
     * Set the path param.
     * <p>
     * The value is encoded using {@link URLEncoder#encode(String, java.nio.charset.Charset)} before it's used to build the
     * target URI.
     *
     * @param name
     * @param value
     * @return self
     * @throws IllegalArgumentException If the client endpoint path does not contain a parameter with the given name
     * @see WebSocketClient#path()
     */
    WebSocketConnector<CLIENT> pathParam(String name, String value);

    /**
     * Add a header used during the initial handshake request.
     *
     * @param name
     * @param value
     * @return self
     * @see HandshakeRequest
     */
    WebSocketConnector<CLIENT> addHeader(String name, String value);

    /**
     * Add the subprotocol.
     *
     * @param name
     * @param value
     * @return self
     */
    WebSocketConnector<CLIENT> addSubprotocol(String value);

    /**
     *
     * @return a new {@link Uni} with a {@link WebSocketClientConnection} item
     */
    @CheckReturnValue
    Uni<WebSocketClientConnection> connect();

    /**
     *
     * @return the client connection
     */
    default WebSocketClientConnection connectAndAwait() {
        return connect().await().indefinitely();
    }

}
