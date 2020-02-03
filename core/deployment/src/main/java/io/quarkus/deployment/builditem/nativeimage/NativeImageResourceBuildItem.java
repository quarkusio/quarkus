package io.quarkus.deployment.builditem.nativeimage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.util.Comparators;

/**
 * A build item that indicates that a static resource should be included in the native image
 */
public final class NativeImageResourceBuildItem extends MultiBuildItem implements Comparable<NativeImageResourceBuildItem> {

    private final List<String> resources;

    public NativeImageResourceBuildItem(String... resources) {
        this.resources = Arrays.asList(resources);
    }

    public NativeImageResourceBuildItem(List<String> resources) {
        this.resources = new ArrayList<>(resources);
    }

    public List<String> getResources() {
        return resources;
    }

    @Override
    public int compareTo(NativeImageResourceBuildItem other) {
        return Comparators.<String> forCollections().compare(this.resources, other.resources);
    }
}
