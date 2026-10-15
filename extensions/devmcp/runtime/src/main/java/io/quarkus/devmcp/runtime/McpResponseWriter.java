package io.quarkus.devmcp.runtime;

import java.nio.charset.StandardCharsets;

import io.quarkus.devjsonrpc.runtime.comms.JsonRpcResponseWriter;
import io.quarkus.devjsonrpc.runtime.comms.MessageType;
import io.quarkus.devjsonrpc.runtime.jsonrpc.json.JsonMapper;
import io.quarkus.devmcp.runtime.model.tool.CallToolResult;
import io.vertx.core.http.HttpServerResponse;

/**
 * Write the response for MCP Call. Depending on the original method request, we might need to wrap this into an expected output
 */
public class McpResponseWriter implements JsonRpcResponseWriter {
    private final HttpServerResponse response;
    private final String requestMethodName;
    private final JsonMapper jsonMapper;

    public McpResponseWriter(HttpServerResponse response, JsonMapper jsonMapper, String requestMethodName) {
        this.response = response;
        this.jsonMapper = jsonMapper;
        this.requestMethodName = requestMethodName;
    }

    @Override
    public void write(String message) {
        String output = message + "\n\n";
        int length = output.getBytes(StandardCharsets.UTF_8).length;

        // Guard on both closed() and ended(): a response may already be ended (e.g. by a route that both wrote a
        // reply and then failed) without being closed yet, and calling end() again would throw.
        if (!response.closed() && !response.ended()) {
            response.putHeader("Content-Type", "application/json")
                    .putHeader("Content-Length", String.valueOf(length))
                    .end(output);
        }
    }

    @Override
    public void close() {
        if (!response.ended()) {
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
        if (requestMethodName.equalsIgnoreCase(McpBuiltinMethods.INITIALIZE) ||
                requestMethodName.equalsIgnoreCase(McpBuiltinMethods.NOTIFICATION) ||
                requestMethodName.equalsIgnoreCase(McpBuiltinMethods.TOOLS_LIST) ||
                requestMethodName.equalsIgnoreCase(McpBuiltinMethods.RESOURCES_LIST) ||
                requestMethodName.equalsIgnoreCase(McpBuiltinMethods.RESOURCES_READ)) {
            return object;
        } else { // Anyting else is a Tools call that we need to wrap in a call result objectc
            String text = jsonMapper.toString(object, true);
            return new CallToolResult(text);
        }
    }
}
