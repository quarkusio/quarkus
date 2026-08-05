package io.quarkus.devjsonrpc.runtime.jsonrpc;

import static io.quarkus.devjsonrpc.runtime.jsonrpc.JsonRpcKeys.ID;
import static io.quarkus.devjsonrpc.runtime.jsonrpc.JsonRpcKeys.JSONRPC;
import static io.quarkus.devjsonrpc.runtime.jsonrpc.JsonRpcKeys.METHOD;
import static io.quarkus.devjsonrpc.runtime.jsonrpc.JsonRpcKeys.PARAMS;

import java.util.Map;

import io.quarkus.devjsonrpc.runtime.jsonrpc.json.JsonMapper;

public class JsonRpcRequestCreator {
    private final JsonMapper jsonMapper;

    JsonRpcRequestCreator(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * This creates a Request from the parsed JSON map
     *
     * @param jsonMap the parsed JSON as a Map
     * @return JsonRpcRequest
     */
    @SuppressWarnings("unchecked")
    public JsonRpcRequest create(Map<String, Object> jsonMap) {
        JsonRpcRequest jsonRpcRequest = new JsonRpcRequest(this.jsonMapper);
        if (jsonMap.containsKey(ID)) {
            jsonRpcRequest.setId(((Number) jsonMap.get(ID)).intValue());
        }
        if (jsonMap.containsKey(JSONRPC)) {
            jsonRpcRequest.setJsonrpc((String) jsonMap.get(JSONRPC));
        }
        jsonRpcRequest.setMethod((String) jsonMap.get(METHOD));
        if (jsonMap.containsKey(PARAMS)) {
            Map<String, Object> map = (Map<String, Object>) jsonMap.get(PARAMS);
            jsonRpcRequest.setParams(map);
        }

        return jsonRpcRequest;
    }

    public String toJson(JsonRpcRequest jsonRpcRequest) {
        return jsonMapper.toString(jsonRpcRequest, true);
    }
}
