package io.quarkus.deployment.builditem.nativeimage;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that indicates that a Java package should be exported using
 * '-J--add-exports' option to become visible to native-image
 */
public final class JPMSExportBuildItem extends MultiBuildItem {
    private final String moduleName;
    private final String packageName;

    public JPMSExportBuildItem(String moduleName, String packageName) {
        this.moduleName = moduleName;
        this.packageName = packageName;
    }

    public String getPackage() {
        return packageName;
    }

    public String getModule() {
        return moduleName;
    }
}
