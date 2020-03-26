
package io.quarkus.container.spi;

import java.util.Optional;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ContainerImageInfoBuildItem extends SimpleBuildItem {

    private static final String SLASH = "/";
    private static final String COLN = ":";

    /**
     * The container registry to use
     */
    public final Optional<String> registry;

    private final String image;

    public ContainerImageInfoBuildItem(Optional<String> registry, Optional<String> group, String name, String tag) {
        this.registry = registry;

        StringBuilder sb = new StringBuilder();
        registry.ifPresent(r -> sb.append(r).append(SLASH));
        group.ifPresent(s -> sb.append(s).append(SLASH));

        sb.append(name).append(COLN).append(tag);
        this.image = sb.toString();
    }

    public Optional<String> getRegistry() {
        return registry;
    }

    public String getImage() {
        return image;
    }
}
