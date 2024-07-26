package io.quarkus.devui.deployment;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Hold add discovered build time methods that can be executed via json-rpc
 */
public final class DeploymentMethodBuildItem extends SimpleBuildItem {

    private final List<String> methods;
    private final List<String> subscriptions;

    public DeploymentMethodBuildItem(List<String> methods, List<String> subscriptions) {
        this.methods = methods;
        this.subscriptions = subscriptions;
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
}
