package io.quarkus.kubernetes.spi;

public final class KubernetesNamespaceBuildItem extends BaseTargetable {

    private final String namespace;

    public KubernetesNamespaceBuildItem(String target, String namespace) {
        super(target);
        this.namespace = namespace;
    }

    public String getNamespace() {
        return namespace;
    }
}
