package io.quarkus.devui.runtime.jsonrpc;

import java.util.Map;

import io.quarkus.devui.runtime.jsonrpc.json.JsonMapper;

public final class JsonRpcRequest {
    private int id;
    private String jsonrpc = JsonRpcKeys.VERSION;
    private String method;
    private Map<String, Object> params;
    private final JsonMapper jsonMapper;

    public JsonRpcRequest(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Map getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public boolean hasParams() {
        return this.params != null && !this.params.isEmpty();
    }

    public boolean hasParam(String key) {
        return this.params != null && this.params.containsKey(key);
    }

    public <T> T getParam(String key, Class<T> paramType) {
        if (hasParam(key)) {
            return jsonMapper.fromValue(params.get(key), paramType);
        }
        return null;
    }

}
