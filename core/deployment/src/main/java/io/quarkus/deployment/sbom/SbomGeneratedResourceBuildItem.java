package io.quarkus.deployment.sbom;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Resource to be included in the application archive as part of SBOM generation.
 * <p>
 * SBOM generators must use this build item instead of
 * {@link io.quarkus.deployment.builditem.GeneratedResourceBuildItem}
 * to avoid build-step cycles with tree-shake analysis, which collects
 * roots from {@code GeneratedResourceBuildItem} instances.
 */
public final class SbomGeneratedResourceBuildItem extends MultiBuildItem {

    private final String name;
    private final byte[] data;

    public SbomGeneratedResourceBuildItem(String name, byte[] data) {
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
