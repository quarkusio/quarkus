package io.quarkus.devui.deployment;

import java.util.List;
import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * Hold add discovered build time methods that can be executed via json-rpc
 */
public final class DeploymentMethodBuildItem extends SimpleBuildItem {

    private final List<String> methods;
    private final List<String> subscriptions;
    private final Map<String, RuntimeValue> recordedValues;

    public DeploymentMethodBuildItem(List<String> methods, List<String> subscriptions,
            Map<String, RuntimeValue> recordedValues) {
        this.methods = methods;
        this.subscriptions = subscriptions;
        this.recordedValues = recordedValues;
    }

    public List<String> getMethods() {
        return this.methods;
    }

    public boolean hasMethods() {
        return this.methods != null && !this.methods.isEmpty();
    }

    public List<String> getSubscriptions() {
        return this.subscriptions;
    }

    public boolean hasSubscriptions() {
        return this.subscriptions != null && !this.subscriptions.isEmpty();
    }

    public Map<String, RuntimeValue> getRecordedValues() {
        return this.recordedValues;
    }

    public boolean hasRecordedValues() {
        return this.recordedValues != null && !this.recordedValues.isEmpty();
    }
}
