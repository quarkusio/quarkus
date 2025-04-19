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

    public JsonRpcCodec(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public JsonRpcRequest readRequest(String json) {
        return new JsonRpcRequest(jsonMapper, (JsonObject) jsonMapper.fromString(json, Object.class));
    }

    public void writeResponse(JsonRpcResponseWriter writer, int id, Object object, MessageType messageType) {
        writeResponse(writer, new JsonRpcResponse(id,
                new JsonRpcResponse.Result(messageType.name(), object)));
    }

    public void writeMethodNotFoundResponse(JsonRpcResponseWriter writer, int id, String jsonRpcMethodName) {
        writeResponse(writer, new JsonRpcResponse(id,
                new JsonRpcResponse.Error(METHOD_NOT_FOUND, "Method [" + jsonRpcMethodName + "] not found")));
    }

    public void writeErrorResponse(JsonRpcResponseWriter writer, int id, String jsonRpcMethodName, Throwable exception) {
        LOG.error("Error in JsonRPC Call", exception);
        writeResponse(writer, new JsonRpcResponse(id,
                new JsonRpcResponse.Error(INTERNAL_ERROR,
                        "Method [" + jsonRpcMethodName + "] failed: " + exception.getMessage())));
    }

    private void writeResponse(JsonRpcResponseWriter writer, JsonRpcResponse response) {
        writer.write(jsonMapper.toString(response, true));
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }
}
