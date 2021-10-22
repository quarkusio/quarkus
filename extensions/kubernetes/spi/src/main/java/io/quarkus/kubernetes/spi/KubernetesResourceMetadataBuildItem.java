
package io.quarkus.kubernetes.spi;

import io.quarkus.builder.item.MultiBuildItem;

public final class KubernetesResourceMetadataBuildItem extends MultiBuildItem {

    private final String target;
    private final String group;
    private final String version;
    private final String kind;
    private final String name;

    public KubernetesResourceMetadataBuildItem(String target, String group, String version, String kind, String name) {
        this.target = target;
        this.group = group;
        this.version = version;
        this.kind = kind;
        this.name = name;
    }

    public String getTarget() {
        return target;
    }

    public String getGroup() {
        return group;
    }

    public String getVersion() {
        return version;
    }

    public String getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }

}
