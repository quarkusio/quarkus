package io.quarkus.devui.runtime.jsonrpc;

import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.ID;
import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.JSONRPC;
import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.METHOD;
import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.PARAMS;
import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.VERSION;

import java.util.Map;

import io.quarkus.devui.runtime.jsonrpc.json.JsonMapper;
import io.vertx.core.json.JsonObject;

public class JsonRpcRequest {

    private final JsonMapper jsonMapper;
    private final JsonObject jsonObject;

    JsonRpcRequest(JsonMapper jsonMapper, JsonObject jsonObject) {
        this.jsonMapper = jsonMapper;
        this.jsonObject = jsonObject;
    }

    public int getId() {
        return jsonObject.getInteger(ID);
    }

    public String getJsonrpc() {
        String value = jsonObject.getString(JSONRPC);
        if (value != null) {
            return value;
        }
        return VERSION;
    }

    public String getMethod() {
        return jsonObject.getString(METHOD);
    }

    public boolean hasParams() {
        return this.getParams() != null;
    }

    public Map<?, ?> getParams() {
        JsonObject paramsObject = jsonObject.getJsonObject(PARAMS);
        if (paramsObject != null && paramsObject.getMap() != null && !paramsObject.getMap().isEmpty()) {
            return paramsObject.getMap();
        }
        return null;
    }

    public <T> T getParam(String key, Class<T> paramType) {
        Map<?, ?> params = getParams();
        if (params == null || !params.containsKey(key)) {
            return null;
        }
        return jsonMapper.fromValue(params.get(key), paramType);
    }

    @Override
    public String toString() {
        return jsonMapper.toString(jsonObject, true);
    }
}
