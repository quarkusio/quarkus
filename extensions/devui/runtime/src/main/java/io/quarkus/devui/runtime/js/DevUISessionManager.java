package io.quarkus.devui.runtime.js;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.quarkus.devjsonrpc.runtime.comms.JsonRpcResponseWriter;
import io.quarkus.devjsonrpc.runtime.comms.MessageType;
import io.quarkus.devjsonrpc.runtime.jsonrpc.JsonRpcCodec;
import io.quarkus.runtime.StartupEvent;

public class DevUISessionManager {

    private static final List<JsonRpcResponseWriter> SESSIONS = Collections.synchronizedList(new ArrayList<>());

    @Inject
    JsonRpcCodec codec;

    public void addSession(JavaScriptResponseWriter writer) {
        SESSIONS.add(writer);
        purge();
    }

    void onStart(@Observes StartupEvent ev) {
        purge();
        for (JsonRpcResponseWriter jrrw : new ArrayList<>(SESSIONS)) {
            if (!jrrw.isClosed()) {
                codec.writeResponse(jrrw, -1, LocalDateTime.now().toString(), MessageType.HotReload);
            }
        }
    }

    public void purge() {
        SESSIONS.removeIf(JsonRpcResponseWriter::isClosed);
    }
}
