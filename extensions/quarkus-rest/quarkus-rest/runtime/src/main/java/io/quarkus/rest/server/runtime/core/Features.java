package io.quarkus.rest.server.runtime.core;

import java.util.ArrayList;
import java.util.List;

import io.quarkus.rest.common.runtime.model.ResourceFeature;

public class Features {

    private final List<ResourceFeature> resourceFeatures = new ArrayList<>();

    public void addFeature(ResourceFeature resourceFeature) {
        resourceFeatures.add(resourceFeature);
    }

    public List<ResourceFeature> getResourceFeatures() {
        return resourceFeatures;
    }
}
