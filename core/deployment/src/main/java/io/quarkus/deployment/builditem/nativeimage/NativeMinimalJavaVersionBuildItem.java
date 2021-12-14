package io.quarkus.deployment.builditem.nativeimage;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that indicates the minimal acceptable JDK version
 * the native-image tool was bundled with.
 */
public final class NativeMinimalJavaVersionBuildItem extends MultiBuildItem {
    public final int minFeature;
    public final int minUpdate;
    public final String warning;

    /**
     * @param minFeature e.g. 17 for JDK 17.0.1
     * @param minUpdate e.g. 1 for JDK 17.0.1
     */
    public NativeMinimalJavaVersionBuildItem(int minFeature, int minUpdate, String warning) {
        this.minFeature = minFeature;
        this.minUpdate = minUpdate;
        this.warning = warning;
    }
}
