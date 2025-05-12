package io.quarkus.devui.deployment;

import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.devui.spi.buildtime.jsonrpc.DeploymentJsonRpcMethod;
import io.quarkus.devui.spi.buildtime.jsonrpc.RecordedJsonRpcMethod;

/**
 * Hold add discovered build time methods that can be executed via json-rpc
 */
public final class DeploymentMethodBuildItem extends SimpleBuildItem {

    private final Map<String, DeploymentJsonRpcMethod> methods;
    private final Map<String, DeploymentJsonRpcMethod> subscriptions;
    private final Map<String, RecordedJsonRpcMethod> recordedMethods;
    private final Map<String, RecordedJsonRpcMethod> recordedSubscriptions;

    public DeploymentMethodBuildItem(Map<String, DeploymentJsonRpcMethod> methods,
            Map<String, DeploymentJsonRpcMethod> subscriptions,
            Map<String, RecordedJsonRpcMethod> recordedMethods, Map<String, RecordedJsonRpcMethod> recordedSubscriptions) {
        this.methods = methods;
        this.subscriptions = subscriptions;
        this.recordedMethods = recordedMethods;
        this.recordedSubscriptions = recordedSubscriptions;
    }

    // Methods

    public Map<String, DeploymentJsonRpcMethod> getMethods() {
        return this.methods;
    }

    public boolean hasMethods() {
        return this.methods != null && !this.methods.isEmpty();
    }

    // Subscriptions

    public Map<String, DeploymentJsonRpcMethod> getSubscriptions() {
        return this.subscriptions;
    }

    public boolean hasSubscriptions() {
        return this.subscriptions != null && !this.subscriptions.isEmpty();
    }

    // Recorded Methods

    public Map<String, RecordedJsonRpcMethod> getRecordedMethods() {
        return this.recordedMethods;
    }

    public boolean hasRecordedMethods() {
        return this.recordedMethods != null && !this.recordedMethods.isEmpty();
    }

    // Recorded Subscriptions

    public Map<String, RecordedJsonRpcMethod> getRecordedSubscriptions() {
        return this.recordedSubscriptions;
    }

    public boolean hasRecordedSubscriptions() {
        return this.recordedSubscriptions != null && !this.recordedSubscriptions.isEmpty();
    }
}
