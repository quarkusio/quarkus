package io.quarkus.neo4j.deployment;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;

/**
 * This implements the protocol version negotiation of bolt. Testing to see if in address will respond to this is a
 * quick way to find out if it's a running bolt server.
 * <p>
 * This class first appeared in https://github.com/michael-simons/junit-jupiter-causal-cluster-testcontainer-extension
 * by Andrew Jefferson and Michael Simons
 */
final class BoltHandshaker {

    private static final int magicToken = 1616949271;

    // Versions message that cannot be matched because it is all zeros.
    private static final byte[] versionsMessage = {
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
    };

    private final String address;
    private final int port;

    BoltHandshaker(String address, int port) {
        this.address = address;
        this.port = port;
    }

    private boolean doBoltHandshake(String address, int port, int timeoutMillis) {

        try (Socket socket = new Socket()) {

            // Set the socket timeout for blocking operations
            socket.setSoTimeout(timeoutMillis);

            // Connects this socket to the server (also with the specified timeout value).
            socket.connect(new InetSocketAddress(address, port), timeoutMillis);

            DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
            DataInputStream dIn = new DataInputStream(socket.getInputStream());

            // Send magic token (0x6060B017)
            dOut.writeInt(magicToken);
            dOut.flush();

            // Send 4 supported versions
            // Except we don't support any versions and communicate that by sending all zeros
            dOut.write(versionsMessage);
            dOut.flush();

            // Receive agreed version
            // It should be 0 because there are no possible versions we can agree on
            int response = dIn.readInt();
            assert response == 0;

            // Because we cannot agree on a version the server should close its side of the connection
            // resulting in EOF (-1) on all subsequent reads.
            return dIn.read() == -1;
        } catch (IOException exception) {
            // Return false if handshake fails
            return false;
        }
    }

    boolean isBoltPortReachable(Duration timeout) {
        int timeoutMillis = Math.toIntExact(timeout.toMillis());
        return doBoltHandshake(address, port, timeoutMillis);
    }
}
