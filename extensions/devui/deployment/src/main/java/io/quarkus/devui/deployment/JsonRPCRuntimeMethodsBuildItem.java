package io.quarkus.devui.deployment;

import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.devui.spi.buildtime.jsonrpc.RuntimeJsonRpcMethod;

/**
 * Simple holder for all discovered Json RPC Runtime Endpoints
 */
public final class JsonRPCRuntimeMethodsBuildItem extends SimpleBuildItem {

    private final Map<String, RuntimeJsonRpcMethod> runtimeMethodsMap;
    private final Map<String, RuntimeJsonRpcMethod> runtimeSubscriptionsMap;

    public JsonRPCRuntimeMethodsBuildItem(Map<String, RuntimeJsonRpcMethod> runtimeMethodsMap,
            Map<String, RuntimeJsonRpcMethod> runtimeSubscriptionsMap) {
        this.runtimeMethodsMap = runtimeMethodsMap;
        this.runtimeSubscriptionsMap = runtimeSubscriptionsMap;
    }

    public Map<String, RuntimeJsonRpcMethod> getRuntimeMethodsMap() {
        return runtimeMethodsMap;
    }

    public Map<String, RuntimeJsonRpcMethod> getRuntimeSubscriptionsMap() {
        return runtimeSubscriptionsMap;
    }
}
