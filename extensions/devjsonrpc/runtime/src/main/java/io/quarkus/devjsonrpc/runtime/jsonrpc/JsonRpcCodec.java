package io.quarkus.devjsonrpc.runtime.jsonrpc;

import static io.quarkus.devjsonrpc.runtime.jsonrpc.JsonRpcKeys.INTERNAL_ERROR;
import static io.quarkus.devjsonrpc.runtime.jsonrpc.JsonRpcKeys.METHOD_NOT_FOUND;

import java.util.Map;

import org.jboss.logging.Logger;

import io.quarkus.devjsonrpc.runtime.comms.JsonRpcResponseWriter;
import io.quarkus.devjsonrpc.runtime.comms.MessageType;
import io.quarkus.devjsonrpc.runtime.jsonrpc.json.JsonMapper;

public final class JsonRpcCodec {
    private static final Logger LOG = Logger.getLogger(JsonRpcCodec.class);
    private final JsonMapper jsonMapper;
    private final JsonRpcRequestCreator jsonRpcRequestCreator;

    public JsonRpcCodec(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
        this.jsonRpcRequestCreator = new JsonRpcRequestCreator(jsonMapper);
    }

    @SuppressWarnings("unchecked")
    public JsonRpcRequest readRequest(String json) {
        Object parsed = jsonMapper.fromString(json, Object.class);
        Map<String, Object> jsonMap;
        if (parsed instanceof Map) {
            jsonMap = (Map<String, Object>) parsed;
        } else {
            // When Vert.x type adapters are active, the mapper returns JsonObject instead of Map
            try {
                java.lang.reflect.Method getMap = parsed.getClass().getMethod("getMap");
                jsonMap = (Map<String, Object>) getMap.invoke(parsed);
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException("Cannot convert parsed JSON to Map: " + parsed.getClass(), e);
            }
        }
        return jsonRpcRequestCreator.create(jsonMap);
    }

    public void writeResponse(JsonRpcResponseWriter writer, int id, Object object, MessageType messageType) {
        Object decoratedObject = writer.decorateObject(object, messageType);
        writeResponse(writer, new JsonRpcResponse(id, decoratedObject));
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
