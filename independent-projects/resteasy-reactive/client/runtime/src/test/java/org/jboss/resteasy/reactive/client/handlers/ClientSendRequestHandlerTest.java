package org.jboss.resteasy.reactive.client.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;

public class ClientSendRequestHandlerTest {

    private static final URI URI = java.net.URI.create("http://localhost:8080/api");

    @Test
    public void sourceAddressExhaustion() {
        SocketException netty = new SocketException("Can't assign requested address: localhost/127.0.0.1:8080");
        netty.initCause(new BindException("Can't assign requested address"));
        String message = ClientSendRequestHandler.connectionFailureMessage(netty, URI);
        assertTrue(message.startsWith("Unable to connect to http://localhost:8080/api: Can't assign requested address"),
                message);
        assertTrue(message.contains("ephemeral ports"), message);
        assertTrue(message.contains("connection-pool-size"), message);
    }

    @Test
    public void connectionRefused() {
        String message = ClientSendRequestHandler.connectionFailureMessage(new ConnectException("Connection refused"), URI);
        assertEquals("Unable to connect to http://localhost:8080/api: Connection refused."
                + " Nothing is listening on the target address, or the connection was rejected by a firewall", message);
    }

    @Test
    public void unknownHost() {
        String message = ClientSendRequestHandler.connectionFailureMessage(new UnknownHostException("nowhere.invalid"), URI);
        assertEquals("Unable to connect to http://localhost:8080/api: nowhere.invalid. The host name could not be resolved",
                message);
    }

    @Test
    public void otherFailureWithoutUri() {
        String message = ClientSendRequestHandler.connectionFailureMessage(new IOException("handshake failed"), null);
        assertEquals("Unable to connect to the remote service: handshake failed", message);
    }
}
