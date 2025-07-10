package io.quarkus.qute.debug.client;

import java.io.IOException;
import java.net.ServerSocket;

public class DebuggerUtils {

    public static int findAvailableSocketPort() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();
            synchronized (serverSocket) {
                try {
                    serverSocket.wait(1L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return port;
        }
    }

}
