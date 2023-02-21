package io.quarkus.devui.runtime.jsonrpc;

import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.ID;
import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.JSONRPC;
import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.METHOD;
import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.PARAMS;
import static io.quarkus.devui.runtime.jsonrpc.JsonRpcKeys.VERSION;

import java.util.Map;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

public class JsonRpcReader {

    private final JsonObject jsonObject;

    private JsonRpcReader(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public static JsonRpcReader read(String json) {
        return new JsonRpcReader((JsonObject) Json.decodeValue(json));
    }

    public int getId() {
        return jsonObject.getInteger(ID);
    }

    public String getJsonrpc() {
        return jsonObject.getString(JSONRPC, VERSION);
    }

    public String getMethod() {
        return jsonObject.getString(METHOD);
    }

    public boolean isMethod(String m) {
        return this.getMethod().equalsIgnoreCase(m);
    }

    public boolean hasParams() {
        return this.getParams() != null;
    }

    public Map<String, ?> getParams() {
        JsonObject paramsObject = jsonObject.getJsonObject(PARAMS);
        if (paramsObject != null && paramsObject.getMap() != null && !paramsObject.getMap().isEmpty()) {
            return paramsObject.getMap();
        }
        return null;
    }

    @SuppressWarnings({ "unchecked", "unchecked" })
    public <T> T getParam(String key) {
        Map<String, ?> params = getParams();
        if (params == null || !params.containsKey(key)) {
            return null;
        }
        return (T) params.get(key);
    }

    @Override
    public String toString() {
        return jsonObject.encodePrettily();
    }

}
