package io.quarkus.resteasy.deployment;

import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.resteasy.runtime.NonJaxRsClassMappings;

/**
 * A build item that holds Non jax-rs classes
 */
public final class NonJaxRsClassBuildItem extends SimpleBuildItem {

    public final Map<String, NonJaxRsClassMappings> nonJaxRsPaths;

    public NonJaxRsClassBuildItem(Map<String, NonJaxRsClassMappings> nonJaxRsPaths) {
        this.nonJaxRsPaths = nonJaxRsPaths;
    }
}
