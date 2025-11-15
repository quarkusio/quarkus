package io.quarkus.devui.runtime.mcp;

import java.util.Map;

import jakarta.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import io.quarkus.devui.runtime.comms.JsonRpcRouter;
import io.quarkus.devui.runtime.comms.MessageType;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcCodec;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcRequest;
import io.quarkus.devui.runtime.jsonrpc.json.JsonMapper;
import io.quarkus.devui.runtime.mcp.model.InitializeResponse;
import io.quarkus.devui.runtime.spi.McpServerConfiguration;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

/**
 * Alternative Json RPC communication using Streamable HTTP for MCP
 */
public class McpHttpHandler implements Handler<RoutingContext> {
    private static final Logger LOG = Logger.getLogger(McpHttpHandler.class.getName());
    private final String quarkusVersion;
    private final JsonMapper jsonMapper;
    private final JsonRpcCodec codec;

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
            if (ctx.request().method().equals(HttpMethod.GET)) { // TODO: Also check Accept header
                handleSSEInitRequest(ctx);
            } else if (ctx.request().method().equals(HttpMethod.POST)) { // TODO: Also check Accept header
                handleMCPJsonRPCRequest(ctx);
            }
        } else {
            ctx.response()
                    .setStatusCode(404)
                    .end("Method Not Found");
        }

    }

    private void handleSSEInitRequest(RoutingContext ctx) {
        // TODO: Add SSE Support
        // The client MAY issue an HTTP GET to the MCP endpoint.
        // This can be used to open an SSE stream, allowing the server to communicate to the client,
        // without the client first sending data via HTTP POST.
        // The client MUST include an Accept header, listing text/event-stream as a supported content type.

        //        ctx.response()
        //                .putHeader("Content-Type", "text/event-stream; charset=utf-8")
        //                .putHeader("Cache-Control", "no-cache")
        //                .putHeader("Connection", "keep-alive")
        //                .setChunked(true);
        //
        //        try {
        //            JsonRpcRouter jsonRpcRouter = CDI.current().select(JsonRpcRouter.class).get();
        //            jsonRpcRouter.addSseSession(ctx);
        //        } catch (IllegalStateException e) {
        //            LOG.debug("Failed to connect to dev sse server", e);
        //            ctx.response().end();
        //        }

        // The server MUST either return Content-Type: text/event-stream in response to this HTTP GET,
        // or else return HTTP 405 Method Not Allowed, indicating that the server does not offer an SSE stream at this endpoint.

        ctx.response()
                .setStatusCode(405)
                .putHeader("Allow", "POST")
                .putHeader("Content-Type", "text/plain")
                .end("Method Not Allowed");

    }

    private void handleMCPJsonRPCRequest(RoutingContext ctx) {
        StringBuilder sb = new StringBuilder();
        ctx.request().handler(buf -> sb.append(buf.toString()));

        ctx.request().endHandler(v -> {
            JsonRpcRouter jsonRpcRouter = CDI.current().select(JsonRpcRouter.class).get();
            String input = sb.toString();

            JsonRpcRequest jsonRpcRequest = codec.readMCPRequest(input);

            String methodName = jsonRpcRequest.getMethod();
            McpResponseWriter writer = new McpResponseWriter(ctx.response(), this.jsonMapper, methodName);
            // First see if this a protocol specific method

            if (methodName.equalsIgnoreCase(McpBuiltinMethods.INITIALIZE)) {
                // This is a MCP server that initialize
                this.routeToMCPInitialize(jsonRpcRequest, codec, writer);
            } else if (methodName.startsWith(McpBuiltinMethods.NOTIFICATION)) {
                // This is a MCP notification
                this.routeToMCPNotification(jsonRpcRequest, codec, writer);
            } else if (methodName.equalsIgnoreCase(McpBuiltinMethods.TOOLS_LIST) ||
                    methodName.equalsIgnoreCase(McpBuiltinMethods.RESOURCES_LIST)) {
                jsonRpcRequest.setMethod(methodName.replace(SLASH, UNDERSCORE));
                // Make sure that parameters is empty as expected.
                jsonRpcRequest.setParams(null);
                jsonRpcRouter.route(jsonRpcRequest, writer);
            } else if (methodName.equalsIgnoreCase(McpBuiltinMethods.RESOURCES_READ)) {
                jsonRpcRequest.setMethod(methodName.replace(SLASH, UNDERSCORE));
                // Make sure that the only parameter is uri (as expected).
                String uri = jsonRpcRequest.getParam("uri", String.class);
                jsonRpcRequest.getParams().clear();
                jsonRpcRequest.setParams(Map.of("uri", uri));
                jsonRpcRouter.route(jsonRpcRequest, writer);
            } else {
                // This is a normal extension method
                jsonRpcRouter.route(jsonRpcRequest, writer);
            }
        });
    }

    private void routeToMCPInitialize(JsonRpcRequest jsonRpcRequest, JsonRpcCodec codec, McpResponseWriter writer) {
        if (jsonRpcRequest.hasParam(CLIENT_INFO)) {
            Map map = jsonRpcRequest.getParam(CLIENT_INFO, Map.class);
            McpDevUIJsonRpcService devMcpJsonRpcService = CDI.current().select(McpDevUIJsonRpcService.class).get();
            devMcpJsonRpcService.addClientInfo(McpClientInfo.fromMap(map));
        }
        String input = jsonMapper.toString(jsonRpcRequest, true);
        codec.writeResponse(writer, jsonRpcRequest.getId(), new InitializeResponse(this.quarkusVersion), MessageType.Void);
    }

    private void routeToMCPNotification(JsonRpcRequest jsonRpcRequest, JsonRpcCodec codec, McpResponseWriter writer) {
        String jsonRpcMethodName = jsonRpcRequest.getMethod();
        String notification = jsonRpcMethodName.substring(McpBuiltinMethods.NOTIFICATION.length() + 2);
        // TODO: Do something with the notification ?

        writer.getResponse().setStatusCode(202).end();
    }

    private static final String SLASH = "/";
    private static final String UNDERSCORE = "_";
    private static final String CLIENT_INFO = "clientInfo";
}
