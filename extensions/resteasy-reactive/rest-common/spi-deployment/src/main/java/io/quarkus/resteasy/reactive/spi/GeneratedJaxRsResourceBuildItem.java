package io.quarkus.resteasy.reactive.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a JAX-RS resource that is generated.
 * Meant to be used by extension that generate JAX-RS resources as part of their build time processing
 */
public final class GeneratedJaxRsResourceBuildItem extends MultiBuildItem {

    private final String name;
    private final byte[] data;

    public GeneratedJaxRsResourceBuildItem(String name, byte[] data) {
        this.name = name;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public byte[] getData() {
        return data;
    }
}
