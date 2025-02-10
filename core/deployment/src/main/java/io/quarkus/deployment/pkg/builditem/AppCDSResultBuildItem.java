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
    /**
     * The type of file generated
     */
    private final JvmStartupOptimizerArchiveType type;

    public AppCDSResultBuildItem(Path appCDS) {
        this(appCDS, JvmStartupOptimizerArchiveType.AppCDS);
    }

    public AppCDSResultBuildItem(Path appCDS, JvmStartupOptimizerArchiveType type) {
        this.appCDS = appCDS;
        this.type = type;
    }

    public Path getAppCDS() {
        return appCDS;
    }

    public JvmStartupOptimizerArchiveType getType() {
        return type;
    }
}
