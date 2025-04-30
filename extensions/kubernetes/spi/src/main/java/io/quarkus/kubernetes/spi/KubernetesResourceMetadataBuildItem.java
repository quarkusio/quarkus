
package io.quarkus.kubernetes.spi;

public final class KubernetesResourceMetadataBuildItem extends BaseTargetable {

    private final String group;
    private final String version;
    private final String kind;
    private final String name;

    public KubernetesResourceMetadataBuildItem(String target, String group, String version, String kind, String name) {
        super(target);
        this.group = group;
        this.version = version;
        this.kind = kind;
        this.name = name;
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
