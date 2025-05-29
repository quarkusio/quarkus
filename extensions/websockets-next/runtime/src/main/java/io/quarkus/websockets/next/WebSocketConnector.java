package io.quarkus.websockets.next;

import java.net.URI;
import java.net.URLEncoder;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;

import io.quarkus.tls.TlsConfiguration;
import io.quarkus.websockets.next.UserData.TypedKey;
import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Uni;

/**
 * A connector can be used to configure and open a new client connection backed by a client endpoint that is used to
 * consume and send messages.
 * <p>
 * Quarkus provides a CDI bean with bean type {@code WebSocketConnector<CLIENT>} and qualifier {@link Default}. The actual type
 * argument of an injection point is used to determine the client endpoint. The type is validated during build
 * and if it does not represent a client endpoint then the build fails.
 * <p>
 * This construct is not thread-safe and should not be used concurrently.
 * <p>
 * Connectors should not be reused. If you need to create multiple connections in a row you'll need to obtain a new connetor
 * instance programmatically using {@link Instance#get()}:
 * <code><pre>
 * import jakarta.enterprise.inject.Instance;
 *
 * &#64;Inject
 * Instance&#60;WebSocketConnector&#60;MyEndpoint&#62;&#62; connector;
 *
 * void connect() {
 *      var connection1 = connector.get().baseUri(uri)
 *                  .addHeader("Foo", "alpha")
 *                  .connectAndAwait();
 *      var connection2 = connector.get().baseUri(uri)
 *                  .addHeader("Foo", "bravo")
 *                  .connectAndAwait();
 * }
 * </pre></code>
 *
 * @param <CLIENT> The client endpoint class
 * @see WebSocketClient
 * @see WebSocketClientConnection
 */
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
     * Set the name of the {@link TlsConfiguration}.
     *
     * @param tlsConfigurationName
     * @return self
     * @see io.quarkus.tls.TlsConfigurationRegistry#get(String)
     */
    WebSocketConnector<CLIENT> tlsConfigurationName(String tlsConfigurationName);

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
     * Add a value to the connection user data.
     *
     * @param key
     * @param value
     * @param <VALUE>
     * @return self
     * @see UserData#put(TypedKey, Object)
     * @see WebSocketClientConnection#userData()
     */
    <VALUE> WebSocketConnector<CLIENT> userData(TypedKey<VALUE> key, VALUE value);

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
