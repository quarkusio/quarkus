package io.quarkus.deployment.builditem.nativeimage;

import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.graal.GraalVM;

/**
 * A build item that indicates that a Java package should be exported using
 * '-J--add-exports' option to become visible to native-image
 */
public final class JPMSExportBuildItem extends MultiBuildItem {
    private final String moduleName;
    private final String packageName;
    private final GraalVM.Version exportSince;
    private final GraalVM.Version exportBefore;

    public JPMSExportBuildItem(String moduleName, String packageName) {
        this(moduleName, packageName, null, null);
    }

    public JPMSExportBuildItem(String moduleName, String packageName, GraalVM.Version exportSince) {
        this(moduleName, packageName, exportSince, null);
    }

    /**
     * Creates a build item that indicates that a Java package should be exported for a specific GraalVM version range.
     *
     * @param moduleName the module name
     * @param packageName the package name
     * @param exportSince the version of GraalVM since which the package should be exported (inclusive)
     * @param exportBefore the version of GraalVM before which the package should be exported (exclusive)
     * @deprecated use {@link #JPMSExportBuildItem(String, String, GraalVM.Version, GraalVM.Version)} instead
     */
    @Deprecated
    public JPMSExportBuildItem(String moduleName, String packageName,
            io.quarkus.deployment.pkg.steps.GraalVM.Version exportSince,
            io.quarkus.deployment.pkg.steps.GraalVM.Version exportBefore) {
        this.moduleName = moduleName;
        this.packageName = packageName;
        this.exportSince = exportSince;
        this.exportBefore = exportBefore;
    }

    /**
     * Creates a build item that indicates that a Java package should be exported for a specific GraalVM version range.
     *
     * @param moduleName the module name
     * @param packageName the package name
     * @param exportSince the version of GraalVM since which the package should be exported (inclusive)
     * @param exportBefore the version of GraalVM before which the package should be exported (exclusive)
     */
    public JPMSExportBuildItem(String moduleName, String packageName, GraalVM.Version exportSince,
            GraalVM.Version exportBefore) {
        this.moduleName = moduleName;
        this.packageName = packageName;
        this.exportSince = exportSince;
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

    public GraalVM.Version getExportSince() {
        return exportSince;
    }

    public GraalVM.Version getExportBefore() {
        return exportBefore;
    }

    public boolean isRequired(GraalVM.Version current) {
        return (exportSince == null || current.compareTo(exportSince) >= 0) &&
                (exportBefore == null || current.compareTo(exportBefore) < 0);
    }
}
