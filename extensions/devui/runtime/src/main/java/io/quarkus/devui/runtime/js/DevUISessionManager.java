package io.quarkus.devui.runtime.js;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.quarkus.devjsonrpc.runtime.comms.JsonRpcResponseWriter;
import io.quarkus.devjsonrpc.runtime.comms.JsonRpcRouter;
import io.quarkus.devjsonrpc.runtime.comms.MessageType;
import io.quarkus.runtime.StartupEvent;

/**
 * Keeps track of the open Dev UI websockets. Adding and purging sessions is static, so the websocket handler does not
 * need a CDI lookup (which could resolve against an application that is shutting down).
 */
public class DevUISessionManager {

    private static final List<JsonRpcResponseWriter> SESSIONS = Collections.synchronizedList(new ArrayList<>());

    @Inject
    JsonRpcRouter jsonRpcRouter;

    public static void addSession(JavaScriptResponseWriter writer) {
        SESSIONS.add(writer);
        purge();
    }

    void onStart(@Observes StartupEvent ev) {
        purge();
        for (JsonRpcResponseWriter jrrw : new ArrayList<>(SESSIONS)) {
            if (!jrrw.isClosed()) {
                jsonRpcRouter.getJsonRpcCodec().writeResponse(jrrw, -1, LocalDateTime.now().toString(),
                        MessageType.HotReload);
            }
        }
    }

    public static void purge() {
        SESSIONS.removeIf(JsonRpcResponseWriter::isClosed);
    }
}
