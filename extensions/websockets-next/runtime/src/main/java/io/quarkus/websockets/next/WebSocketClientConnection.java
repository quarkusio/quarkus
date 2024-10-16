package io.quarkus.websockets.next;

import io.smallrye.common.annotation.Experimental;

/**
 * This interface represents a client connection to a WebSocket endpoint.
 * <p>
 * Quarkus provides a built-in CDI bean that implements this interface and can be injected in a {@link WebSocketClient}
 * endpoint and used to interact with the connected server.
 */
@Experimental("This API is experimental and may change in the future")
public interface WebSocketClientConnection extends Connection {

    /*
     * @return the client id
     */
    String clientId();

}
