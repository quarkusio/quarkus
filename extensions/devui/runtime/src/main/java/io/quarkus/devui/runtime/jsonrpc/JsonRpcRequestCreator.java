package io.quarkus.devui.runtime.jsonrpc;

import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.ID;
import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.JSONRPC;
import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.METHOD;
import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.PARAMS;

import java.util.Map;

import io.quarkus.devui.runtime.jsonrpc.json.JsonMapper;
import io.vertx.core.json.JsonObject;

public class JsonRpcRequestCreator {
    private final JsonMapper jsonMapper;

    JsonRpcRequestCreator(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public JsonRpcRequest create(JsonObject jsonObject) {
        JsonRpcRequest jsonRpcRequest = new JsonRpcRequest();
        jsonRpcRequest.setId(jsonObject.getInteger(ID));
        if (jsonObject.containsKey(JSONRPC)) {
            jsonRpcRequest.setJsonrpc(jsonObject.getString(JSONRPC));
        }
        jsonRpcRequest.setMethod(jsonObject.getString(METHOD));
        if (jsonObject.containsKey(PARAMS)) {
            jsonRpcRequest.setParams(jsonObject.getJsonObject(PARAMS).getMap());
        }

        return jsonRpcRequest;
    }

    // TODO: Repeat of above
    public JsonRpcRequest mcpCreate(JsonObject jsonObject) {
        JsonRpcRequest jsonRpcRequest = new JsonRpcRequest();
        if (jsonObject.containsKey(ID)) {
            jsonRpcRequest.setId(jsonObject.getInteger(ID));
        }
        if (jsonObject.containsKey(JSONRPC)) {
            jsonRpcRequest.setJsonrpc(jsonObject.getString(JSONRPC));
        }

        jsonRpcRequest.setMethod(jsonObject.getString(METHOD));
        if (jsonObject.containsKey(PARAMS)) {

            Map map = jsonObject.getJsonObject(PARAMS).getMap();
            map.remove("_meta");
            map.remove("cursor");
            jsonRpcRequest.setParams(map);
        }

        return remap(jsonRpcRequest);
    }

    public JsonRpcRequest remap(JsonRpcRequest jsonRpcRequest) {
        if (jsonRpcRequest.getMethod().equalsIgnoreCase(TOOLS_SLASH_CALL)) {

            Map params = jsonRpcRequest.getParams();
            String mappedName = (String) params.remove("name");
            Map mappedParams = (Map) params.remove("arguments");

            JsonRpcRequest mapped = new JsonRpcRequest();
            mapped.setId(jsonRpcRequest.getId());
            mapped.setJsonrpc(jsonRpcRequest.getJsonrpc());
            mapped.setMethod(mappedName);
            mapped.setParams(mappedParams);

            return mapped;
        }
        return jsonRpcRequest;
    }

    public String toJson(JsonRpcRequest jsonRpcRequest) {
        return jsonMapper.toString(jsonRpcRequest, true);
    }

    private static final String TOOLS_SLASH_CALL = "tools/call";
}
