package io.quarkus.aesh.websocket.runtime;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds the configured WebSocket terminal path, set at build time via the recorder.
 */
public class AeshWebSocketPath {

    private static final AtomicReference<String> path = new AtomicReference<>("/aesh/terminal");

    public static String getPath() {
        return path.get();
    }

    public static void setPath(String path) {
        AeshWebSocketPath.path.set(path);
    }
}
