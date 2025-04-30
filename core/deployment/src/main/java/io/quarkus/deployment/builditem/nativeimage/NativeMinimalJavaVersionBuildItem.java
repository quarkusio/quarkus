package io.quarkus.deployment.builditem.nativeimage;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that indicates the minimal acceptable JDK version
 * the native-image tool was bundled with.
 */
public final class NativeMinimalJavaVersionBuildItem extends MultiBuildItem {
    public final Runtime.Version minVersion;
    public final String warning;

    /**
     * @param minVersion e.g. 17.0.1
     */
    public NativeMinimalJavaVersionBuildItem(String minVersion, String warning) {
        this.minVersion = Runtime.Version.parse(minVersion);
        this.warning = warning;
    }
}
