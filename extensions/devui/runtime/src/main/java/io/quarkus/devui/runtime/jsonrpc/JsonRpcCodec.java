package io.quarkus.devui.runtime.jsonrpc;

import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.INTERNAL_ERROR;
import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.METHOD_NOT_FOUND;

import org.jboss.logging.Logger;

import io.quarkus.devui.runtime.comms.JsonRpcResponseWriter;
import io.quarkus.devui.runtime.comms.MessageType;
import io.quarkus.devui.runtime.jsonrpc.json.JsonMapper;
import io.vertx.core.json.JsonObject;

public final class JsonRpcCodec {
    private static final Logger LOG = Logger.getLogger(JsonRpcCodec.class);
    private final JsonMapper jsonMapper;
    private final JsonRpcRequestCreator jsonRpcRequestCreator;

    public JsonRpcCodec(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
        this.jsonRpcRequestCreator = new JsonRpcRequestCreator(jsonMapper);
    }

    public JsonRpcRequest readRequest(String json) {
        return jsonRpcRequestCreator.create((JsonObject) jsonMapper.fromString(json, Object.class));
    }

    public JsonRpcRequest readMCPRequest(String json) {
        return jsonRpcRequestCreator.mcpCreate((JsonObject) jsonMapper.fromString(json, Object.class));
    }

    public void writeResponse(JsonRpcResponseWriter writer, Object id, Object object, MessageType messageType) {
        Object decoratedObject = writer.decorateObject(object, messageType);
        writeResponse(writer, new JsonRpcResponse(id, decoratedObject));
    }

    public void writeMethodNotFoundResponse(JsonRpcResponseWriter writer, Object id, String jsonRpcMethodName) {
        writeResponse(writer, new JsonRpcResponse(id,
                new JsonRpcResponse.Error(METHOD_NOT_FOUND, "Method [" + jsonRpcMethodName + "] not found")));
    }

    public void writeErrorResponse(JsonRpcResponseWriter writer, Object id, String jsonRpcMethodName, Throwable exception) {
        LOG.error("Error in JsonRPC Call", exception);
        writeResponse(writer, new JsonRpcResponse(id,
                new JsonRpcResponse.Error(INTERNAL_ERROR,
                        "Method [" + jsonRpcMethodName + "] failed: " + exception.getMessage())));
    }

    /**
     * Writes a JSON-RPC error response with an explicit error code, used when a request cannot be parsed or is not a
     * valid Request object. This must always write something back so a client never hangs waiting for a response.
     */
    public void writeErrorResponse(JsonRpcResponseWriter writer, Object id, int code, String message) {
        writeResponse(writer, new JsonRpcResponse(id, new JsonRpcResponse.Error(code, message)));
    }

    private void writeResponse(JsonRpcResponseWriter writer, JsonRpcResponse response) {
        writer.write(jsonMapper.toString(response, true));
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }
}
