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

    /**
     * This creates a Request as is from Dev UI
     *
     * @param jsonObject the JsonObject from the JS
     * @return JsonRpcRequest
     */
    public JsonRpcRequest create(JsonObject jsonObject) {
        return createWithFilter(jsonObject, null);
    }

    /**
     * This creates a Request from an MCP Client.
     * It might contain meta data in the parameters that is not applicable for the Java method
     *
     * @param jsonObject the JsonObject from the MCP
     * @return JsonRpcRequest
     */
    public JsonRpcRequest mcpCreate(JsonObject jsonObject) {
        return remap(createWithFilter(jsonObject, "_meta", "cursor"));
    }

    /**
     * This remaps a MCP Tool Call to a normal json-rpc method
     *
     * @param jsonRpcRequest the tools call request
     * @return JsonRpcRequest the remapped request
     */
    private JsonRpcRequest remap(JsonRpcRequest jsonRpcRequest) {
        if (jsonRpcRequest.getMethod().equalsIgnoreCase(TOOLS_SLASH_CALL)) {

            Map params = jsonRpcRequest.getParams();
            String mappedName = (String) params.remove("name");
            Map mappedParams = (Map) params.remove("arguments");

            JsonRpcRequest mapped = new JsonRpcRequest(this.jsonMapper);
            mapped.setId(jsonRpcRequest.getId());
            mapped.setJsonrpc(jsonRpcRequest.getJsonrpc());
            mapped.setMethod(mappedName);
            if (mappedParams != null && !mappedParams.isEmpty())
                mapped.setParams(mappedParams);

            return mapped;
        }
        return jsonRpcRequest;
    }

    public String toJson(JsonRpcRequest jsonRpcRequest) {
        return jsonMapper.toString(jsonRpcRequest, true);
    }

    private JsonRpcRequest createWithFilter(JsonObject jsonObject, String... filter) {
        JsonRpcRequest jsonRpcRequest = new JsonRpcRequest(this.jsonMapper);
        if (jsonObject.containsKey(ID)) {
            jsonRpcRequest.setId(jsonObject.getInteger(ID));
        }
        if (jsonObject.containsKey(JSONRPC)) {
            jsonRpcRequest.setJsonrpc(jsonObject.getString(JSONRPC));
        }
        jsonRpcRequest.setMethod(jsonObject.getString(METHOD));
        if (jsonObject.containsKey(PARAMS)) {
            Map<String, Object> map = jsonObject.getJsonObject(PARAMS).getMap();
            if (filter != null) {
                for (String p : filter) {
                    map.remove(p);
                }
            }
            jsonRpcRequest.setParams(map);
        }

        return jsonRpcRequest;
    }

    private static final String TOOLS_SLASH_CALL = "tools/call";
}
