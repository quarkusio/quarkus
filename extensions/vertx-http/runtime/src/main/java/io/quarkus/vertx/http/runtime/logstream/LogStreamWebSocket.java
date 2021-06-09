package io.quarkus.vertx.http.runtime.logstream;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.LogContext;

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
    private final HistoryHandler historyHandler;

    private final String initMessage;

    private final ExtHandler rootHandler;
    private final org.jboss.logmanager.Logger rootLogger;

    public LogStreamWebSocket(HistoryHandler historyHandler) {
        this.historyHandler = historyHandler;
        // Add history handler
        final LogContext logContext = LogContext.getLogContext();
        rootLogger = logContext.getLogger("");
        rootHandler = findCorrectHandler(rootLogger.getHandlers());
        initMessage = createInitMessage();
    }

    @Override
    public void handle(RoutingContext event) {
        if ("websocket".equalsIgnoreCase(event.request().getHeader(HttpHeaderNames.UPGRADE)) && !event.request().isEnded()) {
            event.request().toWebSocket(new Handler<AsyncResult<ServerWebSocket>>() {
                @Override
                public void handle(AsyncResult<ServerWebSocket> event) {
                    if (event.succeeded()) {
                        ServerWebSocket socket = event.result();
                        SessionState state = new SessionState();
                        WebSocketHandler webSocketHandler = new WebSocketHandler(socket);
                        state.handler = webSocketHandler;
                        state.session = socket;
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
                        socket.writeTextMessage(initMessage.toString());
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

    static class SessionState {
        ServerWebSocket session;
        ExtHandler handler;
        boolean started;
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
            rootLogger.addHandler(session.handler);

            // Polulate history
            if (historyHandler.hasHistory()) {
                List<ExtLogRecord> history = historyHandler.getHistory();
                for (ExtLogRecord lr : history) {
                    session.handler.publish(lr);
                }
            }
        }
    }

    private void stop(SessionState session) {
        rootLogger.removeHandler(session.handler);
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

    private void addHandler(ExtHandler extHandler) {
        if (rootHandler != null) {
            rootHandler.addHandler(extHandler);
        } else {
            rootLogger.addHandler(extHandler);
        }
    }

    private void removeHandler(ExtHandler extHandler) {
        if (rootHandler != null) {
            rootHandler.removeHandler(extHandler);
        } else {
            rootLogger.removeHandler(extHandler);
        }
    }

    private ExtHandler findCorrectHandler(java.util.logging.Handler[] handlers) {
        for (java.util.logging.Handler h : handlers) {
            if (h instanceof ExtHandler) {
                ExtHandler exth = (ExtHandler) h;
                ExtHandler consoleLogger = getConsoleHandler(exth.getHandlers());
                if (consoleLogger != null) {
                    return exth;
                }
            }
        }
        // Else return first ext handler
        for (java.util.logging.Handler h : handlers) {
            if (h instanceof ExtHandler) {
                return (ExtHandler) h;
            }
        }
        return null;
    }

    private ExtHandler getConsoleHandler(java.util.logging.Handler[] handlers) {
        if (handlers != null && handlers.length > 0) {
            for (java.util.logging.Handler h : handlers) {

                if (h.getClass().equals(org.jboss.logmanager.handlers.ConsoleHandler.class)) {
                    return (ExtHandler) h;
                }
            }
        }
        return null;
    }

    private String createInitMessage() {
        Json.JsonObjectBuilder initMessage = Json.object();
        initMessage.put(TYPE, INIT);
        initMessage.put("loggers", LogController.getLoggers());
        initMessage.put("levels", LogController.getLevels());
        return initMessage.build();
    }

    private static final String TYPE = "type";
    private static final String INIT = "init";
    private static final String START = "start";
    private static final String STOP = "stop";
    private static final String UPDATE = "update";

}
