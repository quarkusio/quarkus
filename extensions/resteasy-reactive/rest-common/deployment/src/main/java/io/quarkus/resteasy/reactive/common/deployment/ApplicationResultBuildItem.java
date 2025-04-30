package io.quarkus.resteasy.reactive.common.deployment;

import org.jboss.resteasy.reactive.common.processor.scanning.ApplicationScanningResult;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ApplicationResultBuildItem extends SimpleBuildItem {

    final ApplicationScanningResult result;

    public ApplicationResultBuildItem(ApplicationScanningResult result) {
        this.result = result;
    }

    public ApplicationScanningResult getResult() {
        return result;
    }
}
