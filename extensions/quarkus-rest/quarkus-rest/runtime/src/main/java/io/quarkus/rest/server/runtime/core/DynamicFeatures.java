package io.quarkus.rest.server.runtime.core;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.rest.common.runtime.model.ResourceDynamicFeature;

public class DynamicFeatures {

    private final List<ResourceDynamicFeature> resourceDynamicFeatures = new ArrayList<>();

    public void addFeature(ResourceDynamicFeature resourceFeature) {
        resourceDynamicFeatures.add(resourceFeature);
    }

    public List<ResourceDynamicFeature> getResourceDynamicFeatures() {
        return resourceDynamicFeatures;
    }
}
