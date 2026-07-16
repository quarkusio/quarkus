package io.quarkus.devmcp.runtime;

import java.util.Map;

import jakarta.enterprise.inject.spi.CDI;

import io.quarkus.devjsonrpc.runtime.comms.JsonRpcRouter;
import io.quarkus.devjsonrpc.runtime.comms.MessageType;
import io.quarkus.devjsonrpc.runtime.jsonrpc.JsonRpcCodec;
import io.quarkus.devjsonrpc.runtime.jsonrpc.JsonRpcMethod;
import io.quarkus.devjsonrpc.runtime.jsonrpc.JsonRpcRequest;
import io.quarkus.devjsonrpc.runtime.jsonrpc.json.JsonMapper;
import io.quarkus.devmcp.runtime.model.InitializeResponse;
import io.quarkus.devmcp.spi.McpServerConfiguration;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

/**
 * Alternative Json RPC communication using Streamable HTTP for MCP
 */
public class McpHttpHandler implements Handler<RoutingContext> {
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
            //The client MUST use HTTP POST to send JSON-RPC messages to the MCP endpoint.
            //The client MUST include an Accept header, listing both application/json and text/event-stream as supported content types.
            // The body of the POST request MUST be a single JSON-RPC request, notification, or response.
            if (ctx.request().method().equals(HttpMethod.GET)) { // TODO: Also check Accept header
                handleSSEInitRequest(ctx);
            } else if (ctx.request().method().equals(HttpMethod.POST)) { // TODO: Also check Accept header
                handleMCPJsonRPCRequest(ctx);
            } else {
                ctx.response()
                        .setStatusCode(405)
                        .putHeader("Allow", "GET, POST")
                        .putHeader("Content-Type", "text/plain")
                        .end("Method Not Allowed");
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

            JsonRpcRequest jsonRpcRequest = parseMcpRequest(input);

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
                if (isMethodEnabled(jsonRpcRouter, jsonRpcRequest)) {
                    jsonRpcRouter.route(jsonRpcRequest, writer);
                } else {
                    codec.writeMethodNotFoundResponse(writer, jsonRpcRequest.getId(), methodName);
                }
            }
        });
    }

    /**
     * Parse an MCP JSON-RPC request from raw JSON input.
     * This handles MCP-specific preprocessing: filtering out _meta and cursor parameters,
     * and remapping tools/call requests to standard JSON-RPC method calls.
     */
    @SuppressWarnings("unchecked")
    private JsonRpcRequest parseMcpRequest(String input) {
        JsonRpcRequest request = codec.readRequest(input);

        // Filter out MCP-specific metadata from params
        Map<String, Object> params = request.getParams();
        if (params != null) {
            params.remove("_meta");
            params.remove("cursor");
        }

        // Remap tools/call to a standard method call
        return remapToolsCall(request);
    }

    /**
     * Remaps a MCP tools/call request to a normal JSON-RPC method call.
     * The tools/call format uses {"method": "tools/call", "params": {"name": "...", "arguments": {...}}}
     * which gets remapped to {"method": "<name>", "params": <arguments>}.
     */
    @SuppressWarnings("unchecked")
    private JsonRpcRequest remapToolsCall(JsonRpcRequest request) {
        if (request.getMethod().equalsIgnoreCase(TOOLS_SLASH_CALL)) {
            Map params = request.getParams();
            if (params == null) {
                return request;
            }
            String mappedName = (String) params.remove("name");
            if (mappedName == null) {
                return request;
            }
            Map mappedParams = (Map) params.remove("arguments");

            JsonRpcRequest mapped = new JsonRpcRequest(this.jsonMapper);
            mapped.setId(request.getId());
            mapped.setJsonrpc(request.getJsonrpc());
            mapped.setMethod(mappedName);
            if (mappedParams != null && !mappedParams.isEmpty())
                mapped.setParams(mappedParams);

            return mapped;
        }
        return request;
    }

    private void routeToMCPInitialize(JsonRpcRequest jsonRpcRequest, JsonRpcCodec codec, McpResponseWriter writer) {
        if (jsonRpcRequest.hasParam(CLIENT_INFO)) {
            Map map = jsonRpcRequest.getParam(CLIENT_INFO, Map.class);
            McpDevUIJsonRpcService devMcpJsonRpcService = CDI.current().select(McpDevUIJsonRpcService.class).get();
            devMcpJsonRpcService.addClientInfo(McpClientInfo.fromMap(map));
        }
        codec.writeResponse(writer, jsonRpcRequest.getId(), new InitializeResponse(this.quarkusVersion), MessageType.Void);
    }

    private void routeToMCPNotification(JsonRpcRequest jsonRpcRequest, JsonRpcCodec codec, McpResponseWriter writer) {
        String jsonRpcMethodName = jsonRpcRequest.getMethod();
        String notification = jsonRpcMethodName.substring(McpBuiltinMethods.NOTIFICATION.length() + 2);
        // TODO: Do something with the notification ?

        writer.getResponse().setStatusCode(202).end();
    }

    // Unsubscribe is not handled here — it's a WebSocket-only concept with no meaning over HTTP.
    private boolean isMethodEnabled(JsonRpcRouter router, JsonRpcRequest request) {
        McpDevUIJsonRpcService mcpService = CDI.current().select(McpDevUIJsonRpcService.class).get();
        JsonRpcMethod m = router.findMethod(request.getMethod());
        return m != null && mcpService.isEnabled(m, Filter.enabled);
    }

    private static final String SLASH = "/";
    private static final String UNDERSCORE = "_";
    private static final String CLIENT_INFO = "clientInfo";
    private static final String TOOLS_SLASH_CALL = "tools/call";
}
