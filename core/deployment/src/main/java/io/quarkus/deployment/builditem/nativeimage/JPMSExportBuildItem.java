package io.quarkus.deployment.builditem.nativeimage;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.pkg.steps.GraalVM;

/**
 * A build item that indicates that a Java package should be exported using
 * '-J--add-exports' option to become visible to native-image
 */
public final class JPMSExportBuildItem extends MultiBuildItem {
    private final String moduleName;
    private final String packageName;
    private final GraalVM.Version exportAfter;
    private final GraalVM.Version exportBefore;

    public JPMSExportBuildItem(String moduleName, String packageName) {
        this(moduleName, packageName, null, null);
    }

    public JPMSExportBuildItem(String moduleName, String packageName, GraalVM.Version exportAfter) {
        this(moduleName, packageName, exportAfter, null);
    }

    public JPMSExportBuildItem(String moduleName, String packageName, GraalVM.Version exportAfter,
            GraalVM.Version exportBefore) {
        this.moduleName = moduleName;
        this.packageName = packageName;
        this.exportAfter = exportAfter;
        this.exportBefore = exportBefore;
    }

    public String getPackage() {
        return packageName;
    }

    public String getModule() {
        return moduleName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JPMSExportBuildItem that = (JPMSExportBuildItem) o;
        return moduleName.equals(that.moduleName) && packageName.equals(that.packageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduleName, packageName);
    }

    public GraalVM.Version getExportAfter() {
        return exportAfter;
    }

    public GraalVM.Version getExportBefore() {
        return exportBefore;
    }

    public boolean isRequired(GraalVM.Version current) {
        return (exportAfter == null || current.compareTo(exportAfter) > 0) &&
                (exportBefore == null || current.compareTo(exportBefore) < 0);
    }
}
