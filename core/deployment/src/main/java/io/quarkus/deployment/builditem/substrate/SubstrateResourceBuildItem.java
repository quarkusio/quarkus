package io.quarkus.deployment.builditem.substrate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that indicates that a static resource should be included in the native image
 * 
 * @deprecated Use {@link io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem NativeImageResourceBuildItem}
 *             instead.
 */
@Deprecated
public final class SubstrateResourceBuildItem extends MultiBuildItem {

    private final List<String> resources;

    public SubstrateResourceBuildItem(String... resources) {
        this.resources = Arrays.asList(resources);
    }

    public SubstrateResourceBuildItem(List<String> resources) {
        this.resources = new ArrayList<>(resources);
    }

    public List<String> getResources() {
        return resources;
    }
}
