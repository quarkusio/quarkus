package io.quarkus.mailer.runtime;

import java.net.ServerSocket;

final class SocketUtil {

    private SocketUtil() {
    }

    static int findAvailablePort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (Exception e) {
            // return a default port
            return 25347;
        }
    }
}
