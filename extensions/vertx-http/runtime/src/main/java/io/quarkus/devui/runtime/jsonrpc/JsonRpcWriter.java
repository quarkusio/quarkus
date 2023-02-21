package io.quarkus.devui.runtime.jsonrpc;

import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.CODE;
import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.ERROR;
import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.ID;
import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.JSONRPC;
import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.MESSAGE;
import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.MESSAGE_TYPE;
import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.METHOD_NOT_FOUND;
import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.MessageType;
import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.OBJECT;
import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.RESULT;
import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.VERSION;

import io.vertx.core.json.JsonObject;

public class JsonRpcWriter {

    private JsonRpcWriter() {
    }

    public static JsonObject writeResponse(int id, Object object, MessageType messageType) {
        JsonObject result = JsonObject.of();
        if (object != null) {
            result.put(OBJECT, object);
        }
        result.put(MESSAGE_TYPE, messageType.name());

        return JsonObject.of(
                ID, id,
                JSONRPC, VERSION,
                RESULT, result);
    }

    public static JsonObject writeMethodNotFoundResponse(int id, String jsonRpcMethodName) {
        JsonObject jsonRpcError = JsonObject.of(
                CODE, METHOD_NOT_FOUND,
                MESSAGE, "Method [" + jsonRpcMethodName + "] not found");

        return JsonObject.of(
                ID, id,
                JSONRPC, VERSION,
                ERROR, jsonRpcError);
    }

}
