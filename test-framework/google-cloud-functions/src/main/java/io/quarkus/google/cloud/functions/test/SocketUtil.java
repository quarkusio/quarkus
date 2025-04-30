package io.quarkus.google.cloud.functions.test;

import java.io.IOException;
import java.net.ServerSocket;

// copied from the mailer extension: io.quarkus.mailer.runtime.SocketUtil
final class SocketUtil {

    private SocketUtil() {
    }

    static int findAvailablePort() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(0);
            return serverSocket.getLocalPort();
        } catch (Exception e) {
            // return a default port
            return 25347;
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
