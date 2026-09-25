package io.quarkus.uberjar.spi;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;
import io.smallrye.common.constraint.Assert;

/**
 * A build item representing the modular UberJar.
 */
public final class UberJarBuildItem extends SimpleBuildItem {
    private final Path jarPath;

    /**
     * Construct a new instance.
     *
     * @param jarPath the output jar path (must not be {@code null})
     */
    public UberJarBuildItem(Path jarPath) {
        this.jarPath = Assert.checkNotNullParam("jarPath", jarPath);
    }

    /**
     * {@return the output jar path (not {@code null})}
     */
    public Path jarPath() {
        return jarPath;
    }
}
