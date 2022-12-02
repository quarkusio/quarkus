package io.quarkus.deployment.builditem;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.Feature;

/**
 * Represents a functionality provided by an extension. The name of the feature gets displayed in the log during application
 * bootstrap.
 * <p>
 * An extension should provide at most one feature. The name must be unique. If multiple extensions register a feature of the
 * same name the build fails.
 * <p>
 * The name of the feature should only contain lowercase characters, words are separated by dash {@code -}; e.g.
 * {@code security-jpa}. Features provided by core extensions should be listed in the {@link Feature} enum.
 */
public final class FeatureBuildItem extends MultiBuildItem {

    private final String name;

    public FeatureBuildItem(Feature feature) {
        this(feature.getName());
    }

    public FeatureBuildItem(String name) {
        this.name = Objects.requireNonNull(name);
    }

    /**
     * The name that gets displayed in the log.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

}