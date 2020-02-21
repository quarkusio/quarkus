
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

    /**
     * The group the container image will be part of
     */
    public final String group;

    /**
     * The name of the container image. If not set defaults to the application name
     */
    public final String name;

    /**
     * The tag of the container image. If not set defaults to the application
     * version
     */
    public final String tag;

    private final String image;

    public ContainerImageInfoBuildItem(Optional<String> registry, String group, String name, String tag) {
        this.registry = registry;
        this.group = group;
        this.name = name;
        this.tag = tag;

        StringBuilder sb = new StringBuilder();
        registry.ifPresent(r -> sb.append(r).append(SLASH));
        sb.append(group).append(SLASH);

        sb.append(name).append(COLN).append(tag);
        this.image = sb.toString();
    }

    public Optional<String> getRegistry() {
        return registry;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getTag() {
        return tag;
    }

    public String getImage() {
        return image;
    }
}
