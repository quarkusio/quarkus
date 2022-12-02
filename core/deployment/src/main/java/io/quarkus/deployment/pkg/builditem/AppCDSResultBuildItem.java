package io.quarkus.deployment.pkg.builditem;

import java.nio.file.Path;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item containing the result of the AppCDS generation process
 */
public final class AppCDSResultBuildItem extends SimpleBuildItem {

    /**
     * The file containing the generated AppCDS
     */
    private final Path appCDS;

    public AppCDSResultBuildItem(Path appCDS) {
        this.appCDS = appCDS;
    }

    public Path getAppCDS() {
        return appCDS;
    }
}
