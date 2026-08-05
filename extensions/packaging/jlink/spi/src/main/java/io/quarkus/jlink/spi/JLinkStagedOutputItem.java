package io.quarkus.jlink.spi;

import java.nio.file.Path;
import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;
import io.smallrye.common.constraint.Assert;

/**
 * A build item which represents the result of staging all {@code jlink} boot artifacts.
 */
public final class JLinkStagedOutputItem extends SimpleBuildItem {
    private final Path stagedOutputPath;
    private final Map<String, Path> bootModulePath;

    /**
     * Construct a new instance.
     *
     * @param stagedOutputPath the staged output path base directory (must not be {@code null})
     * @param bootModulePath the path of each patched JAR for the boot module path, by module name (must not be {@code null})
     */
    public JLinkStagedOutputItem(final Path stagedOutputPath, final Map<String, Path> bootModulePath) {
        this.stagedOutputPath = Assert.checkNotNullParam("stagedOutputPath", stagedOutputPath);
        this.bootModulePath = Map.copyOf(Assert.checkNotNullParam("bootModulePath", bootModulePath));
    }

    /**
     * {@return the staged output path base directory (not {@code null})}
     */
    public Path stagedOutputPath() {
        return stagedOutputPath;
    }

    /**
     * {@return the path of each patched JAR for the boot module path, by module name (not {@code null})}
     */
    public Map<String, Path> bootModulePath() {
        return bootModulePath;
    }
}
