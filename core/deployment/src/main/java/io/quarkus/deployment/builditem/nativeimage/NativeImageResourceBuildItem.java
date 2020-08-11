package io.quarkus.deployment.builditem.nativeimage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that indicates that a static resource should be included in the native image
 */
public final class NativeImageResourceBuildItem extends MultiBuildItem {

    private final List<String> resources;

    private List<String> resourcePatterns;
    private String identifier;

    public NativeImageResourceBuildItem(String... resources) {
        this.resources = Arrays.asList(resources);
    }

    public NativeImageResourceBuildItem(List<String> resources) {
        this.resources = new ArrayList<>(resources);
    }

    public void addResourcePatterns(String identifier, String... resourcePatterns) {
        this.resourcePatterns = Arrays.asList(resourcePatterns);
        this.identifier = identifier;
    }

    public void addResourcePatterns(String... resourcePatterns) {
        addResourcePatterns(null, resourcePatterns);
    }

    public List<String> getResourcePatterns() {
        return resourcePatterns;
    }

    public String getIdentifier() {
        return identifier;
    }

    public List<String> getResources() {
        return resources;
    }
}
