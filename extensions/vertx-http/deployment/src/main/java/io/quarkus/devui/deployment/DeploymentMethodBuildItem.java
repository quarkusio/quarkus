package io.quarkus.devui.deployment;

import java.util.List;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Hold add discovered build time methods that can be executed via json-rpc
 */
public final class DeploymentMethodBuildItem extends SimpleBuildItem {

    private final List<String> methods;

    public DeploymentMethodBuildItem(List<String> methods) {
        this.methods = methods;
    }

    public List<String> getMethods() {
        return this.methods;
    }

    public boolean hasMethods() {
        return this.methods != null && !this.methods.isEmpty();
    }
}
