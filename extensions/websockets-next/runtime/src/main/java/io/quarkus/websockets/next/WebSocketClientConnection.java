package io.quarkus.websockets.next;

/**
 * This interface represents a client connection to a WebSocket endpoint.
 * <p>
 * Quarkus provides a CDI bean that implements this interface and can be injected in a {@link WebSocketClient}
 * endpoint and used to interact with the connected server.
 */
public interface WebSocketClientConnection extends Connection {

    /*
     * @return the client id
     */
    String clientId();

}
