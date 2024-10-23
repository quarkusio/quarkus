package io.quarkus.deployment.builditem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Represents a possible user project root dir path.
 * <p>
 * This is intended for extensions that wants to know what is the project root dir path.
 */
public final class UserProjectRootBuildItem extends SimpleBuildItem {

    private final Path rootDir;

    public UserProjectRootBuildItem(final Path rootDir) {
        this.rootDir = rootDir;
    }

    /**
     * Gets the root dir as {@link Optional}.
     * <p>
     *
     * @return {@link Optional#empty()} if the <code>rootDir</code> is null or is not a directory, otherwise,
     *         returns a non-empty {@link Optional}.
     */
    public Optional<Path> rootDir() {
        if (rootDir == null) {
            return Optional.empty();
        }

        if (!Files.isDirectory(rootDir)) {
            return Optional.empty();
        }

        return Optional.of(rootDir);
    }

}
