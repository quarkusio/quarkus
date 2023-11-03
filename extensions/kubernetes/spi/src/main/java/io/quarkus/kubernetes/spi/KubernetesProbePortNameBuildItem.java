package io.quarkus.kubernetes.spi;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item for selecting which port to use for probes using an {@literal HTTP get} action.
 */
public final class KubernetesProbePortNameBuildItem extends SimpleBuildItem {

    private final String name;
    private final String scheme;

    public KubernetesProbePortNameBuildItem(String name) {
        this(name, null);
    }

    public KubernetesProbePortNameBuildItem(String name, String scheme) {
        this.name = name;
        this.scheme = scheme;
    }

    public String getName() {
        return name;
    }

    public String getScheme() {
        return scheme;
    }
}
