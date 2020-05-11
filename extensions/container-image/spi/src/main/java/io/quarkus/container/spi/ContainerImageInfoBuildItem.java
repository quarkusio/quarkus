
package io.quarkus.container.spi;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ContainerImageInfoBuildItem extends SimpleBuildItem {

    private static final String SLASH = "/";
    private static final String COLN = ":";

    /**
     * The container registry to use
     */
    public final Optional<String> registry;

    private final String imagePrefix;

    private final String tag;

    private final Set<String> additionalTags;

    public ContainerImageInfoBuildItem(Optional<String> registry, Optional<String> group, String name, String tag,
            List<String> additionalTags) {
        this.registry = registry;

        StringBuilder sb = new StringBuilder();
        registry.ifPresent(r -> sb.append(r).append(SLASH));
        group.ifPresent(s -> sb.append(s).append(SLASH));
        this.imagePrefix = sb.append(name).toString();
        this.tag = tag;
        this.additionalTags = new HashSet<>(additionalTags);
    }

    public Optional<String> getRegistry() {
        return registry;
    }

    public String getImage() {
        return imagePrefix + COLN + tag;
    }

    public String getTag() {
        return tag;
    }

    public List<String> getAdditionalImageTags() {
        return getAdditionalTags().stream().map(tag -> imagePrefix + COLN + tag).collect(Collectors.toList());
    }

    public Set<String> getAdditionalTags() {
        return additionalTags;
    }
}
