package io.quarkus.jlink.spi;

import java.nio.file.Path;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;
import io.smallrye.common.constraint.Assert;

/**
 * A build item representing the jlink image.
 */
public final class JLinkImageBuildItem extends SimpleBuildItem {
    private final Path imagePath;
    private final Set<String> launcherNames;

    /**
     * Construct a new instance.
     *
     * @param imagePath the image path (must not be {@code null})
     * @param launcherNames the set of launcher names in the image (must not be {@code null})
     */
    public JLinkImageBuildItem(Path imagePath, Set<String> launcherNames) {
        this.imagePath = Assert.checkNotNullParam("imagePath", imagePath);
        this.launcherNames = Set.copyOf(Assert.checkNotNullParam("launcherNames", launcherNames));
    }

    /**
     * {@return the image path (not {@code null})}
     */
    public Path imagePath() {
        return imagePath;
    }

    /**
     * {@return the set of launcher names in the image (not {@code null})}
     */
    public Set<String> launcherNames() {
        return launcherNames;
    }
}
