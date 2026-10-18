package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;

/**
 * A build item that indicates whether native monitoring is enabled and which option from {@link NativeConfig.MonitoringOption}.
 * To be used in the native image generation.
 */
public final class NativeMonitoringBuildItem extends MultiBuildItem {
    private final NativeConfig.MonitoringOption option;

    public NativeMonitoringBuildItem(NativeConfig.MonitoringOption option) {
        this.option = option;
    }

    public NativeConfig.MonitoringOption getOption() {
        return this.option;
    }
}
