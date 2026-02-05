package io.quarkus.jlink.spi;

import java.nio.file.Path;
import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;
import io.smallrye.common.constraint.Assert;

public final class JLinkImageBuildItem extends SimpleBuildItem {
    private final Path imagePath;
    private final Set<String> launcherNames;

    public JLinkImageBuildItem(Path imagePath, Set<String> launcherNames) {
        this.imagePath = Assert.checkNotNullParam("imagePath", imagePath);
        this.launcherNames = Set.copyOf(Assert.checkNotNullParam("launcherNames", launcherNames));
    }

    public Path imagePath() {
        return imagePath;
    }

    public Set<String> launcherNames() {
        return launcherNames;
    }
}
