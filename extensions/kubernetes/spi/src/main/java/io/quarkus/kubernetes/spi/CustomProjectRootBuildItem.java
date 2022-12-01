package io.quarkus.kubernetes.spi;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Build item that allows us to supply a custom project root instead of allowing dekorate to figure out the project root
 * based on its own rules
 */
public final class CustomProjectRootBuildItem extends SimpleBuildItem {

    private final Path root;

    public CustomProjectRootBuildItem(Path root) {
        this.root = root;
    }

    public Path getRoot() {
        return root;
    }
}
