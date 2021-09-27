package io.quarkus.vertx.http.runtime.logstream;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.quarkus.vertx.http.runtime.devmode.Json;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.RoutingContext;

/**
 * Stream the log in json format over ws
 */
public class LogStreamWebSocket implements Handler<RoutingContext> {
    private static final Logger log = Logger.getLogger(LogStreamWebSocket.class.getName());
    private final WebSocketLogHandler webSocketHandler;

    public LogStreamWebSocket(WebSocketLogHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public void handle(RoutingContext event) {
        if ("websocket".equalsIgnoreCase(event.request().getHeader(HttpHeaderNames.UPGRADE)) && !event.request().isEnded()) {
            event.request().toWebSocket(new Handler<AsyncResult<ServerWebSocket>>() {
                @Override
                public void handle(AsyncResult<ServerWebSocket> event) {
                    if (event.succeeded()) {
                        ServerWebSocket socket = event.result();
                        SessionState state = new SessionState(socket);

                        socket.closeHandler(new Handler<Void>() {
                            @Override
                            public void handle(Void event) {
                                stop(state);
                            }
                        });
                        socket.textMessageHandler(new Handler<String>() {
                            @Override
                            public void handle(String event) {
                                onMessage(event, state);
                            }
                        });
                        socket.writeTextMessage(createInitMessage());
                        start(state);
                    } else {
                        log.log(Level.SEVERE, "Failed to connect to log server", event.cause());
                    }
                }
            });
        } else {
            event.next();
        }
    }

    public void onMessage(String message, SessionState session) {
        if (message != null && !message.isEmpty()) {
            if (message.equalsIgnoreCase(START)) {
                start(session);
            } else if (message.equalsIgnoreCase(STOP)) {
                stop(session);
            } else if (message.startsWith(UPDATE)) {
                update(message);
            }
        }
    }

    private void start(SessionState session) {
        if (!session.started) {
            session.started = true;
            webSocketHandler.addSession(session.id, session.session);
        }
    }

    private void stop(SessionState session) {
        webSocketHandler.removeSession(session.id);
        session.started = false;
    }

    private void update(String message) {
        String[] p = message.split("\\|");
        if (p.length == 3) {
            String loggerName = p[1];
            String levelVal = p[2];
            LogController.updateLogLevel(loggerName, levelVal);
        }
    }

    private String createInitMessage() {
        Json.JsonObjectBuilder initMessage = Json.object();
        initMessage.put(TYPE, INIT);
        initMessage.put("loggers", LogController.getLoggers());
        initMessage.put("levels", LogController.getLevels());
        return initMessage.build();
    }

    static class SessionState {
        ServerWebSocket session;
        String id;
        boolean started;

        public SessionState(ServerWebSocket session) {
            this.session = session;
            this.id = UUID.randomUUID().toString();
        }
    }

    private static final String TYPE = "type";
    private static final String INIT = "init";
    private static final String START = "start";
    private static final String STOP = "stop";
    private static final String UPDATE = "update";

}
