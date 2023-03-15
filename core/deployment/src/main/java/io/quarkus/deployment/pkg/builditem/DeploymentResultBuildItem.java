
package io.quarkus.deployment.pkg.builditem;

import java.util.Map;
import java.util.Optional;

import io.quarkus.builder.item.SimpleBuildItem;

public final class DeploymentResultBuildItem extends SimpleBuildItem {

    private final String platform;
    private final String kind;
    private final String name;
    private final Optional<String> url;
    private final Map<String, String> labels;

    public DeploymentResultBuildItem(String platfrom, String kind, String name, Optional<String> url,
            Map<String, String> labels) {
        this.platform = platfrom;
        this.kind = kind;
        this.name = name;
        this.url = url;
        this.labels = labels;
    }

    public String getPlatform() {
        return platform;
    }

    public String getKind() {
        return kind;
    }

    public String getName() {
        return this.name;
    }

    public Map<String, String> getLabels() {
        return this.labels;
    }

    public Optional<String> getUrl() {
        return url;
    }
}
