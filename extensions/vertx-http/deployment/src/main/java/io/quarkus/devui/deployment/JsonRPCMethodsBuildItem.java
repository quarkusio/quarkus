package io.quarkus.devui.deployment;

import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcMethod;
import io.quarkus.devui.runtime.jsonrpc.JsonRpcMethodName;

/**
 * Simple holder for all discovered Json RPC Methods
 */
public final class JsonRPCMethodsBuildItem extends SimpleBuildItem {

    private final Map<String, Map<JsonRpcMethodName, JsonRpcMethod>> extensionMethodsMap;

    public JsonRPCMethodsBuildItem(Map<String, Map<JsonRpcMethodName, JsonRpcMethod>> extensionMethodsMap) {
        this.extensionMethodsMap = extensionMethodsMap;
    }

    public Map<String, Map<JsonRpcMethodName, JsonRpcMethod>> getExtensionMethodsMap() {
        return extensionMethodsMap;
    }
}
