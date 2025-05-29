package io.quarkus.websockets.next;

import java.net.URI;
import java.net.URLEncoder;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;

import io.quarkus.arc.Arc;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.websockets.next.UserData.TypedKey;
import io.smallrye.common.annotation.CheckReturnValue;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;

/**
 * A basic connector can be used to configure and open a new client connection. Unlike with {@link WebSocketConnector} a
 * client endpoint is not used to consume and send messages.
 * <p>
 * Quarkus provides a CDI bean with bean type {@code BasicWebSocketConnector} and qualifier {@link Default}.
 * <p>
 * This construct is not thread-safe and should not be used concurrently.
 * <p>
 * Connectors should not be reused. If you need to create multiple connections in a row you'll need to obtain a new connetor
 * instance programmatically using {@link Instance#get()}:
 * <code><pre>
 * import jakarta.enterprise.inject.Instance;
 *
 * &#64;Inject
 * Instance&#60;BasicWebSocketConnector&#62; connector;
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
 * @see WebSocketClientConnection
 */
public interface BasicWebSocketConnector {

    /**
     * Obtains a new basic connector. An alternative to {@code @Inject BasicWebSocketConnector}.
     *
     * @return a new basic connector
     */
    static BasicWebSocketConnector create() {
        return Arc.container().instance(BasicWebSocketConnector.class).get();
    }

    /**
     * Set the base URI.
     *
     * @param uri
     * @return self
     */
    BasicWebSocketConnector baseUri(URI uri);

    /**
     * Set the base URI.
     *
     * @param baseUri
     * @return self
     */
    default BasicWebSocketConnector baseUri(String baseUri) {
        return baseUri(URI.create(baseUri));
    }

    /**
     * Set the name of the {@link TlsConfiguration}.
     *
     * @param tlsConfigurationName
     * @return self
     * @see io.quarkus.tls.TlsConfigurationRegistry#get(String)
     */
    BasicWebSocketConnector tlsConfigurationName(String tlsConfigurationName);

    /**
     * Set the path that should be appended to the path of the URI set by {@link #baseUri(URI)}.
     * <p>
     * The path may contain path parameters as defined by {@link WebSocketClient#path()}. In this case, the
     * {@link #pathParam(String, String)} method must be used to pass path param values.
     *
     * @param path
     * @return self
     */
    BasicWebSocketConnector path(String path);

    /**
     * Set the path param.
     * <p>
     * The value is encoded using {@link URLEncoder#encode(String, java.nio.charset.Charset)} before it's used to build the
     * target URI.
     *
     * @param name
     * @param value
     * @return self
     * @throws IllegalArgumentException If the path set by {@link #path(String)} does not contain a parameter with the given
     *         name
     */
    BasicWebSocketConnector pathParam(String name, String value);

    /**
     * Add a header used during the initial handshake request.
     *
     * @param name
     * @param value
     * @return self
     * @see HandshakeRequest
     */
    BasicWebSocketConnector addHeader(String name, String value);

    /**
     * Add the subprotocol.
     *
     * @param name
     * @param value
     * @return self
     */
    BasicWebSocketConnector addSubprotocol(String value);

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
    <VALUE> BasicWebSocketConnector userData(TypedKey<VALUE> key, VALUE value);

    /**
     * Set the execution model for callback handlers.
     * <p>
     * By default, {@link ExecutionModel#BLOCKING} is used.
     *
     * @return self
     * @see #onTextMessage(BiConsumer)
     * @see #onBinaryMessage(BiConsumer)
     * @see #onPong(BiConsumer)
     * @see #onOpen(Consumer)
     * @see #onClose(BiConsumer)
     * @see #onError(BiConsumer)
     */
    BasicWebSocketConnector executionModel(ExecutionModel model);

    /**
     * Set a callback to be invoked when a connection to the server is open.
     *
     * @param consumer
     * @return self
     * @see #executionModel(ExecutionModel)
     */
    BasicWebSocketConnector onOpen(Consumer<WebSocketClientConnection> consumer);

    /**
     * Set a callback to be invoked when a text message is received from the server.
     *
     * @param consumer
     * @return self
     * @see #executionModel(ExecutionModel)
     */
    BasicWebSocketConnector onTextMessage(BiConsumer<WebSocketClientConnection, String> consumer);

    /**
     * Set a callback to be invoked when a binary message is received from the server.
     *
     * @param consumer
     * @return self
     * @see #executionModel(ExecutionModel)
     */
    BasicWebSocketConnector onBinaryMessage(BiConsumer<WebSocketClientConnection, Buffer> consumer);

    /**
     * Set a callback to be invoked when a ping message is received from the server.
     *
     * @param consumer
     * @return self
     * @see #executionModel(ExecutionModel)
     */
    BasicWebSocketConnector onPing(BiConsumer<WebSocketClientConnection, Buffer> consumer);

    /**
     * Set a callback to be invoked when a pong message is received from the server.
     *
     * @param consumer
     * @return self
     * @see #executionModel(ExecutionModel)
     */
    BasicWebSocketConnector onPong(BiConsumer<WebSocketClientConnection, Buffer> consumer);

    /**
     * Set a callback to be invoked when a connection to the server is closed.
     *
     * @param consumer
     * @return self
     * @see #executionModel(ExecutionModel)
     */
    BasicWebSocketConnector onClose(BiConsumer<WebSocketClientConnection, CloseReason> consumer);

    /**
     * Set a callback to be invoked when an error occurs.
     *
     * @param consumer
     * @return self
     * @see #executionModel(ExecutionModel)
     */
    BasicWebSocketConnector onError(BiConsumer<WebSocketClientConnection, Throwable> consumer);

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

    enum ExecutionModel {
        /**
         * Callback may block the current thread.
         */
        BLOCKING,
        /**
         * Callback is executed on the event loop and may not block the current thread.
         */
        NON_BLOCKING,
        /**
         * Callback is executed on a virtual thread.
         */
        VIRTUAL_THREAD,
    }

}
