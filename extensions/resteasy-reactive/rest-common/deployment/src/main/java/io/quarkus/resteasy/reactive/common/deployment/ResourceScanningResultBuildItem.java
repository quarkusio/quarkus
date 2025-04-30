package io.quarkus.resteasy.reactive.common.deployment;

import org.jboss.resteasy.reactive.common.processor.scanning.ResourceScanningResult;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ResourceScanningResultBuildItem extends SimpleBuildItem {

    final ResourceScanningResult result;

    public ResourceScanningResultBuildItem(ResourceScanningResult result) {
        this.result = result;
    }

    public ResourceScanningResult getResult() {
        return result;
    }
}
