package io.quarkus.neo4j.deployment;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Optional;

/**
 * Please keep it public to make the Quarkus class loader happy during test.
 */
public final class PortUtils {

    public static boolean isFree(int port) {
        try (ServerSocket ignored = new ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static Optional<Integer> findFreePort() {

        // There is always a chance that the port number will be allocated between
        // the moment it was free and when the container is started, but that's a
        // risk to agree on to enable a frictionless Neo4j browser experience without using
        // a fixed bolt port.
        try (var socket = new ServerSocket(0)) {
            return Optional.of(socket.getLocalPort());
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private PortUtils() {
    }
}
