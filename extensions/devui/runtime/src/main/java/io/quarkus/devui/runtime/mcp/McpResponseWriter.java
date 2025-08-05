package io.quarkus.devui.runtime.mcp;

import java.nio.charset.StandardCharsets;

import io.quarkus.devui.runtime.comms.JsonRpcResponseWriter;
import io.quarkus.devui.runtime.comms.MessageType;
import io.quarkus.devui.runtime.jsonrpc.json.JsonMapper;
import io.quarkus.devui.runtime.mcp.model.tool.CallToolResult;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;

/**
 * Writes MCP responses either as:
 * - Standard HTTP JSON response
 * - SSE event stream (data: ...)
 */
public class McpResponseWriter implements JsonRpcResponseWriter {
    private final HttpServerResponse response;
    private final JsonMapper jsonMapper;
    private final String requestMethodName;
    private final boolean sse;

    private McpResponseWriter(HttpServerResponse response, JsonMapper jsonMapper, String requestMethodName, boolean sse) {
        this.response = response;
        this.jsonMapper = jsonMapper;
        this.requestMethodName = requestMethodName;
        this.sse = sse;
    }

    /** Factory for HTTP mode */
    public static McpResponseWriter http(HttpServerResponse response, JsonMapper mapper, String method) {
        return new McpResponseWriter(response, mapper, method, false);
    }

    /** Factory for SSE mode */
    public static McpResponseWriter sse(HttpServerResponse response, JsonMapper mapper) {
        return new McpResponseWriter(response, mapper, null, true);
    }

    @Override
    public void write(String message) {
        String output = sse ? "data: " + message + "\n\n" : message + "\n\n";

        if (!response.closed()) {
            if (!sse) {
                // Classic HTTP JSON-RPC
                byte[] bytes = output.getBytes(StandardCharsets.UTF_8);
                response.putHeader("Content-Type", "application/json")
                        .putHeader("Content-Length", String.valueOf(bytes.length))
                        .end(Buffer.buffer(bytes));
            } else {
                // SSE mode: just write chunk
                response.write(output);
            }
        }
    }

    @Override
    public void close() {
        if (!sse && !response.closed()) {
            response.end();
        }
    }

    @Override
    public boolean isOpen() {
        return !response.closed();
    }

    @Override
    public boolean isClosed() {
        return response.closed();
    }

    public HttpServerResponse getResponse() {
        return this.response;
    }

    @Override
    public Object decorateObject(Object object, MessageType messageType) {
        if (requestMethodName == null
                || requestMethodName.equalsIgnoreCase(McpBuiltinMethods.INITIALIZE)
                || requestMethodName.equalsIgnoreCase(McpBuiltinMethods.NOTIFICATION)
                || requestMethodName.equalsIgnoreCase(McpBuiltinMethods.TOOLS_LIST)
                || requestMethodName.equalsIgnoreCase(McpBuiltinMethods.RESOURCES_LIST)
                || requestMethodName.equalsIgnoreCase(McpBuiltinMethods.RESOURCES_READ)) {
            return object;
        } else { // Anyting else is a Tools call that we need to wrap in a call result objectc
            String text = jsonMapper.toString(object, true);
            return new CallToolResult(text);
        }
    }
}
