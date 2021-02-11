package io.quarkus.resteasy.reactive.server.deployment;

import org.jboss.resteasy.reactive.server.processor.scanning.MethodScanner;

import io.quarkus.builder.item.MultiBuildItem;

public final class MethodScannerBuildItem extends MultiBuildItem {

    private final MethodScanner methodScanner;

    public MethodScannerBuildItem(MethodScanner methodScanner) {
        this.methodScanner = methodScanner;
    }

    public MethodScanner getMethodScanner() {
        return methodScanner;
    }
}
