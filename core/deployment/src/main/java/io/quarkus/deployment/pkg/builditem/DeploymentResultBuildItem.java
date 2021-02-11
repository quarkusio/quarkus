
package io.quarkus.deployment.pkg.builditem;

import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;

public final class DeploymentResultBuildItem extends SimpleBuildItem {

    private final String name;
    private final Map<String, String> labels;

    public DeploymentResultBuildItem(String name, Map<String, String> labels) {
        this.name = name;
        this.labels = labels;
    }

    public String getName() {
        return this.name;
    }

    public Map<String, String> getLabels() {
        return this.labels;
    }

}
