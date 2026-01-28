package io.quarkus.aesh.websocket.runtime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;

import io.quarkus.aesh.runtime.AeshRemoteConnectionHandler;
import io.quarkus.aesh.runtime.TransportSessionInfo;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;

/**
 * WebSocket endpoint that provides browser-based terminal access to aesh CLI applications.
 * <p>
 * Uses the JSON protocol from {@link org.aesh.terminal.http.HttpTtyConnection}:
 * <ul>
 * <li>{@code init} - Client capability reporting (terminal type, size, color depth)</li>
 * <li>{@code read} - User terminal input</li>
 * <li>{@code resize} - Terminal size changes</li>
 * </ul>
 * <p>
 * Each WebSocket connection gets its own aesh console runner with independent
 * readline state, while sharing CDI-managed command implementations.
 */
@WebSocket(path = "/aesh/terminal")
public class AeshWebSocketEndpoint implements TransportSessionInfo {

    private static final org.jboss.logging.Logger LOG = org.jboss.logging.Logger.getLogger(AeshWebSocketEndpoint.class);

    @Inject
    AeshRemoteConnectionHandler connectionHandler;

    @Inject
    AeshWebSocketRuntimeConfig runtimeConfig;

    private final ConcurrentHashMap<String, AeshWebSocketConnection> connections = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastActivity = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("aesh-ws");
        return t;
    });

    private volatile ScheduledExecutorService idleScheduler;

    public int getActiveConnectionCount() {
        return connections.size();
    }

    @Override
    public String getTransportName() {
        return "websocket";
    }

    @Override
    public int getActiveSessionCount() {
        return connections.size();
    }

    @Override
    public int getMaxSessions() {
        return runtimeConfig.maxConnections().orElse(-1);
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @OnOpen
    void onOpen(WebSocketConnection ws) {
        // Connection will be fully initialized on first "init" message from xterm.js client
    }

    @OnTextMessage
    void onMessage(String message, WebSocketConnection ws) {
        AeshWebSocketConnection conn = connections.get(ws.id());
        if (conn == null) {
            // Max connections check
            int max = runtimeConfig.maxConnections().orElse(0);
            if (max > 0 && connections.size() >= max) {
                LOG.warnf("Rejected WebSocket connection: limit of %d reached", max);
                ws.closeAndAwait();
                return;
            }

            // First message should be "init" -- create the aesh connection
            conn = new AeshWebSocketConnection(ws);
            connections.put(ws.id(), conn);
            lastActivity.put(ws.id(), System.currentTimeMillis());
            conn.writeToDecoder(message);

            ensureIdleSchedulerStarted();

            // Start command processing on a dedicated thread
            // (AeshConsoleRunner.start() calls openBlocking() which blocks until close)
            AeshWebSocketConnection finalConn = conn;
            try {
                executor.submit(() -> connectionHandler.handle(finalConn, "websocket"));
            } catch (RejectedExecutionException e) {
                LOG.warn("WebSocket executor has been shut down, rejecting new connection");
                connections.remove(ws.id());
                lastActivity.remove(ws.id());
                finalConn.close();
            }
        } else {
            lastActivity.put(ws.id(), System.currentTimeMillis());
            conn.writeToDecoder(message);
        }
    }

    @OnClose
    void onClose(WebSocketConnection ws) {
        lastActivity.remove(ws.id());
        AeshWebSocketConnection conn = connections.remove(ws.id());
        if (conn != null) {
            conn.close();
        }
    }

    private void ensureIdleSchedulerStarted() {
        long idleTimeoutMs = runtimeConfig.idleTimeout().map(d -> d.toMillis()).orElse(0L);
        if (idleTimeoutMs <= 0 || idleScheduler != null) {
            return;
        }
        synchronized (this) {
            if (idleScheduler != null) {
                return;
            }
            idleScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "aesh-ws-idle-checker");
                t.setDaemon(true);
                return t;
            });
            long checkInterval = Math.max(idleTimeoutMs / 2, 500);
            idleScheduler.scheduleAtFixedRate(() -> {
                long now = System.currentTimeMillis();
                for (Map.Entry<String, Long> entry : lastActivity.entrySet()) {
                    if (now - entry.getValue() > idleTimeoutMs) {
                        String id = entry.getKey();
                        LOG.infof("Closing idle WebSocket session %s (timeout: %dms)", id, idleTimeoutMs);
                        AeshWebSocketConnection conn = connections.get(id);
                        if (conn != null) {
                            conn.close();
                        }
                    }
                }
            }, checkInterval, checkInterval, TimeUnit.MILLISECONDS);
        }
    }

    @PreDestroy
    void shutdown() {
        if (idleScheduler != null) {
            idleScheduler.shutdownNow();
        }
        executor.shutdownNow();
    }
}
