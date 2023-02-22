package io.quarkus.devui.runtime.logstream;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import io.quarkus.arc.Arc;
import io.smallrye.mutiny.Multi;
import io.vertx.core.json.JsonObject;

/**
 * This makes the log file available via json RPC
 */
public class LogStreamJsonRPCService {

    public String ping() {
        return "pong";
    }

    public List<JsonObject> history() {
        LogStreamBroadcaster logStreamBroadcaster = Arc.container().instance(LogStreamBroadcaster.class).get();
        LinkedBlockingQueue<JsonObject> history = logStreamBroadcaster.getHistory();
        return new ArrayList<>(history);
    }

    public Multi<JsonObject> streamLog() {
        LogStreamBroadcaster logStreamBroadcaster = Arc.container().instance(LogStreamBroadcaster.class).get();
        return logStreamBroadcaster.getLogStream();
    }

}
