package io.quarkus.vertx.http.runtime.logstream;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;

/**
 * Log handler for Logger Manager
 */
public class HistoryHandler extends ExtHandler {
    private final LinkedBlockingQueue<ExtLogRecord> history;

    public HistoryHandler() {
        this(50);
    }

    public HistoryHandler(int size) {
        this.history = new LinkedBlockingQueue<>(size);
        setLevel(Level.INFO);
        setFormatter(new JsonFormatter());
    }

    @Override
    public final void doPublish(final ExtLogRecord record) {
        // Don't log empty messages
        if (record.getMessage() == null || record.getMessage().isEmpty()) {
            return;
        }

        if (isLoggable(record)) {
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
