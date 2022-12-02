package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Build item to indicate to the various steps that AppCDS generation
 * has been requested
 */
public final class AppCDSRequestedBuildItem extends SimpleBuildItem {

    /**
     * Directory where various files needed for AppCDS generation will reside
     */
    private final Path appCDSDir;

    public AppCDSRequestedBuildItem(Path appCDSDir) {
        this.appCDSDir = appCDSDir;
    }

    public Path getAppCDSDir() {
        return appCDSDir;
    }
}
