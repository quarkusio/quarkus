package io.quarkus.aesh.runtime.devui;

import java.time.LocalDateTime;
import java.util.LinkedList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.inject.Instance;

import io.quarkus.aesh.runtime.AeshCommandMetadata;
import io.quarkus.aesh.runtime.AeshContext;
import io.quarkus.aesh.runtime.AeshSessionEvent;
import io.quarkus.aesh.runtime.SessionClosed;
import io.quarkus.aesh.runtime.SessionOpened;
import io.quarkus.aesh.runtime.TransportSessionInfo;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * JsonRPC service for the Aesh Dev UI pages.
 * <p>
 * Provides command metadata, transport session info, and streams
 * session lifecycle events in real-time.
 */
@ApplicationScoped
public class AeshJsonRPCService {

    private static final int MAX_EVENT_LOG = 100;

    private final Instance<AeshContext> aeshContext;
    private final Instance<TransportSessionInfo> transports;
    private final BroadcastProcessor<JsonObject> sessionEvents;
    private final LinkedList<JsonObject> eventLog = new LinkedList<>();

    public AeshJsonRPCService(Instance<AeshContext> aeshContext, Instance<TransportSessionInfo> transports) {
        this.aeshContext = aeshContext;
        this.transports = transports;
        this.sessionEvents = BroadcastProcessor.create();
    }

    void onSessionOpened(@ObservesAsync @SessionOpened AeshSessionEvent event) {
        addEvent("opened", event);
    }

    void onSessionClosed(@ObservesAsync @SessionClosed AeshSessionEvent event) {
        addEvent("closed", event);
    }

    private void addEvent(String type, AeshSessionEvent event) {
        JsonObject entry = new JsonObject()
                .put("timestamp", LocalDateTime.now().toString())
                .put("eventType", type)
                .put("transport", event.transport())
                .put("sessionId", event.sessionId());

        synchronized (eventLog) {
            eventLog.addLast(entry);
            if (eventLog.size() > MAX_EVENT_LOG) {
                eventLog.removeFirst();
            }
        }

        sessionEvents.onNext(entry);
    }

    /**
     * Returns current transport session info and recent event log.
     */
    public JsonObject getSessionInfo() {
        JsonObject result = new JsonObject();

        JsonArray transportArray = new JsonArray();
        if (transports.isResolvable()) {
            for (TransportSessionInfo transport : transports) {
                transportArray.add(new JsonObject()
                        .put("name", transport.getTransportName())
                        .put("active", transport.getActiveSessionCount())
                        .put("max", transport.getMaxSessions())
                        .put("running", transport.isRunning()));
            }
        }
        result.put("transports", transportArray);

        JsonArray logArray;
        synchronized (eventLog) {
            logArray = new JsonArray(new java.util.ArrayList<>(eventLog));
        }
        result.put("eventLog", logArray);

        return result;
    }

    /**
     * Streams session lifecycle events in real-time.
     */
    public Multi<JsonObject> streamSessionEvents() {
        return sessionEvents;
    }

    /**
     * Returns metadata for all discovered commands.
     */
    public JsonObject getCommands() {
        JsonObject result = new JsonObject();

        if (aeshContext.isResolvable()) {
            AeshContext ctx = aeshContext.get();
            result.put("mode", ctx.getMode().name());

            JsonArray commandsArray = new JsonArray();
            for (AeshCommandMetadata cmd : ctx.getCommands()) {
                JsonObject cmdJson = new JsonObject()
                        .put("name", cmd.getCommandName())
                        .put("description", cmd.getDescription() != null ? cmd.getDescription() : "")
                        .put("className", cmd.getClassName())
                        .put("groupCommand", cmd.isGroupCommand())
                        .put("topCommand", cmd.isTopCommand())
                        .put("cliCommand", cmd.isCliCommand());

                if (cmd.getSubCommandClassNames() != null && !cmd.getSubCommandClassNames().isEmpty()) {
                    cmdJson.put("subCommands", new JsonArray(cmd.getSubCommandClassNames()));
                }

                commandsArray.add(cmdJson);
            }
            result.put("commands", commandsArray);
        } else {
            result.put("mode", "unknown");
            result.put("commands", new JsonArray());
        }

        return result;
    }
}
