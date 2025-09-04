package io.quarkus.devui.runtime.mcp;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.quarkus.devui.runtime.comms.JsonRpcRouter;
import io.quarkus.devui.runtime.comms.MessageType;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcCodec;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcRequest;
import io.quarkus.devui.runtime.jsonrpc.json.JsonMapper;
import io.quarkus.devui.runtime.mcp.model.InitializeResponse;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

/**
 * Hybrid MCP handler:
 * - POST handles normal JSON-RPC for Goose
 * - GET opens SSE stream for async MCP events
 */
public class McpHttpHandler implements Handler<RoutingContext> {
    private static final Logger LOG = Logger.getLogger(McpHttpHandler.class.getName());
    private final String quarkusVersion;
    private final JsonMapper jsonMapper;
    private final JsonRpcCodec codec;
    private volatile McpResponseWriter activeSession;
    private volatile Vertx vertx;
    private volatile long pingTimerId = -1;

    public McpHttpHandler(String quarkusVersion, JsonMapper jsonMapper) {
        this.quarkusVersion = quarkusVersion;
        this.jsonMapper = jsonMapper;
        this.codec = new JsonRpcCodec(jsonMapper);
    }

    @Override
    public void handle(RoutingContext ctx) {
        McpServerConfiguration mcpServerConfiguration = CDI.current().select(McpServerConfiguration.class).get();
        if (mcpServerConfiguration.isEnabled()) {
            // TODO:
            // Servers MUST validate the Origin header on all incoming connections to prevent DNS rebinding attacks
            // When running locally, servers SHOULD bind only to localhost (127.0.0.1) rather than all network interfaces (0.0.0.0)
            // Servers SHOULD implement proper authentication for all connections

            //The client MUST use HTTP POST to send JSON-RPC messages to the MCP endpoint.
            //The client MUST include an Accept header, listing both application/json and text/event-stream as supported content types.
            // The body of the POST request MUST be a single JSON-RPC request, notification, or response.
            HttpMethod method = ctx.request().method();
            if (method.equals(HttpMethod.GET)) {
                handleSSEInitRequest(ctx);
            } else if (method.equals(HttpMethod.POST)) {
                handleMCPJsonRPCRequest(ctx);
            }
        } else {
            ctx.response()
                    .setStatusCode(404)
                    .putHeader("Allow", "GET, POST")
                    .end("Method Not Found");
        }
    }

    private void handleSSEInitRequest(RoutingContext ctx) {
        String accept = ctx.request().getHeader("Accept");
        if (accept == null || !accept.contains("text/event-stream")) {
            ctx.response().setStatusCode(406).end("Accept: text/event-stream required");
            return;
        }

        if (activeSession != null && activeSession.isOpen()) {
            activeSession.close();
        }

        ctx.response()
                .setChunked(true)
                .putHeader("Content-Type", "text/event-stream; charset=utf-8")
                .putHeader("Cache-Control", "no-cache")
                .putHeader("Connection", "keep-alive");

        activeSession = McpResponseWriter.sse(ctx.response(), jsonMapper);

        ctx.response().closeHandler(v -> {
            activeSession = null;
            cancelPingTimer();
        });
        ctx.response().write(": connected\n\n");

        startPingTimer();
    }

    private void handleMCPJsonRPCRequest(RoutingContext ctx) {
        StringBuilder sb = new StringBuilder();
        ctx.request().handler(buf -> sb.append(buf.toString()));

        ctx.request().endHandler(v -> {
            JsonRpcRouter jsonRpcRouter = CDI.current().select(JsonRpcRouter.class).get();
            String input = sb.toString();

            JsonRpcRequest jsonRpcRequest = codec.readMCPRequest(input);
            String methodName = jsonRpcRequest.getMethod();

            McpResponseWriter httpWriter = McpResponseWriter.http(ctx.response(), jsonMapper, methodName);

            if (McpBuiltinMethods.INITIALIZE.equalsIgnoreCase(methodName)) {
                routeToMCPInitialize(jsonRpcRequest, codec, httpWriter);
            } else if (methodName.startsWith(McpBuiltinMethods.NOTIFICATION)) {
                routeToMCPNotification(jsonRpcRequest, codec, httpWriter);
            } else if (methodName.equalsIgnoreCase(McpBuiltinMethods.TOOLS_LIST)
                    || methodName.equalsIgnoreCase(McpBuiltinMethods.RESOURCES_LIST)
                    || methodName.equalsIgnoreCase(McpBuiltinMethods.RESOURCES_READ)) {
                jsonRpcRequest.setMethod(methodName.replace(SLASH, UNDERSCORE));
                jsonRpcRouter.route(jsonRpcRequest, httpWriter);
            } else {
                jsonRpcRouter.route(jsonRpcRequest, httpWriter);
            }
        });
    }

    private void routeToMCPInitialize(JsonRpcRequest jsonRpcRequest, JsonRpcCodec codec,
            McpResponseWriter writer) {
        if (jsonRpcRequest.hasParam(CLIENT_INFO)) {
            Map map = jsonRpcRequest.getParam(CLIENT_INFO, Map.class);
            DevMcpJsonRpcService devMcpJsonRpcService = CDI.current().select(DevMcpJsonRpcService.class).get();
            devMcpJsonRpcService.addClientInfo(McpClientInfo.fromMap(map));
        }

        codec.writeResponse(writer, jsonRpcRequest.getId(),
                new InitializeResponse(this.quarkusVersion), MessageType.Void);

        routeToActiveSSESession(jsonRpcRequest, codec,
                new InitializeResponse(this.quarkusVersion), MessageType.Void);
    }

    private void routeToMCPNotification(JsonRpcRequest jsonRpcRequest, JsonRpcCodec codec,
            McpResponseWriter writer) {
        String jsonRpcMethodName = jsonRpcRequest.getMethod();
        String notification = jsonRpcMethodName.substring(McpBuiltinMethods.NOTIFICATION.length() + 2);

        // Acknowledge HTTP POST with 202
        writer.getResponse().setStatusCode(202).end();

        // Send to SSE if active session exist
        routeToActiveSSESession(jsonRpcRequest, codec, jsonRpcRequest, MessageType.Void);
    }

    private void routeToActiveSSESession(JsonRpcRequest request, JsonRpcCodec codec, Object payload, MessageType type) {
        if (activeSession != null && activeSession.isOpen()) {
            codec.writeResponse(activeSession, request.getId(), payload, type);
        }
    }

    private void startPingTimer() {
        cancelPingTimer();
        pingTimerId = getVertx().setPeriodic(PING_INTERVAL_MS, id -> {
            McpResponseWriter writer = activeSession;
            if (writer != null && writer.isOpen()) {
                try {
                    writer.getResponse().write(": ping\n\n");
                } catch (Exception e) {
                    LOG.debug("Ping failed, closing session", e);
                    writer.close();
                    activeSession = null;
                    cancelPingTimer();
                }
            } else {
                cancelPingTimer();
            }
        });
    }

    private void cancelPingTimer() {
        if (pingTimerId != -1) {
            getVertx().cancelTimer(pingTimerId);
            pingTimerId = -1;
        }
    }

    private Vertx getVertx() {
        if (vertx == null) {
            vertx = CDI.current().select(Vertx.class).get();
        }
        return vertx;
    }

    private static final String SLASH = "/";
    private static final String UNDERSCORE = "_";
    private static final String CLIENT_INFO = "clientInfo";
    private static final long PING_INTERVAL_MS = TimeUnit.SECONDS.toMillis(15);
}
