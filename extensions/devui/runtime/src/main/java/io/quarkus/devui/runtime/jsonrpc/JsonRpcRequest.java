package io.quarkus.devui.runtime.jsonrpc;

import java.util.Map;

public final class JsonRpcRequest {
    private int id;
    private String jsonrpc = JsonRpcKeys.VERSION;
    private String method;
    private Map params;

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

    public void setParams(Map params) {
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
            return (T) this.params.get(key);
        }
        return null;
    }

}
