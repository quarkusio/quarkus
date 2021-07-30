package io.quarkus.vertx.http.runtime.logstream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;

import io.vertx.core.http.ServerWebSocket;

/**
 * Log handler for Logger Manager
 */
public class WebSocketLogHandler extends ExtHandler {
    private final LinkedBlockingQueue<ExtLogRecord> history;

    private static final Map<String, ServerWebSocket> SESSIONS = new ConcurrentHashMap<>();

    public WebSocketLogHandler() {
        this(50);
    }

    public WebSocketLogHandler(int size) {
        this.history = new LinkedBlockingQueue<>(size);
        setFormatter(new JsonFormatter());
    }

    @Override
    public final void doPublish(final ExtLogRecord record) {
        // Don't log empty messages
        if (record.getMessage() == null || record.getMessage().isEmpty()) {
            return;
        }

        if (isLoggable(record)) {
            recordHistory(record);
            streamToWebSocket(record);
        }
    }

    public void addSession(String id, ServerWebSocket session) {
        SESSIONS.put(id, session);

        // Polulate history
        if (hasHistory()) {
            List<ExtLogRecord> history = getHistory();
            for (ExtLogRecord lr : history) {
                streamToWebSocket(lr);
            }
        }
    }

    public void removeSession(String id) {
        if (SESSIONS.containsKey(id)) {
            SESSIONS.remove(id);
        }
    }

    private void recordHistory(final ExtLogRecord record) {
        synchronized (this) {
            try {
                if (history.remainingCapacity() == 0) {
                    history.take();
                }
                history.add(record);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void streamToWebSocket(final ExtLogRecord record) {
        if (!SESSIONS.isEmpty()) {
            String message = getFormatter().format(record);

            for (ServerWebSocket session : SESSIONS.values()) {
                try {
                    session.writeTextMessage(message);
                } catch (Throwable ex) {
                    session.close();
                }
            }
        }
    }

    public boolean hasHistory() {
        return !history.isEmpty();
    }

    public List<ExtLogRecord> getHistory() {
        return new ArrayList<>(history);
    }

    public void clearHistory() {
        this.history.clear();
    }
}
