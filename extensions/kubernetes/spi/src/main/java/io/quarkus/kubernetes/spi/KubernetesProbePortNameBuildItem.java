package io.quarkus.kubernetes.spi;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item for selecting which port to use for probes using an {@literal HTTP get} action.
 */
public class KubernetesProbePortNameBuildItem extends SimpleBuildItem {

    private final String name;

    public KubernetesProbePortNameBuildItem(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
