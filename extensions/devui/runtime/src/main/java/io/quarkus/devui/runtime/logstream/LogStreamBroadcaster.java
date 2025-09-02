package io.quarkus.devui.runtime.logstream;

import java.util.concurrent.LinkedBlockingQueue;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class LogStreamBroadcaster {

    private final LinkedBlockingQueue<JsonObject> history = new LinkedBlockingQueue<>(60);
    private final BroadcastProcessor<JsonObject> logStream = BroadcastProcessor.create();

    public BroadcastProcessor<JsonObject> getLogStream() {
        return this.logStream;
    }

    public void onNext(JsonObject message) {
        recordHistory(message);
        this.logStream.onNext(message);
    }

    public LinkedBlockingQueue<JsonObject> getHistory() {
        return history;
    }

    private void recordHistory(final JsonObject message) {
        synchronized (this) {
            try {
                if (history.remainingCapacity() == 0) {
                    history.take();
                }
                history.add(message);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
