package org.jboss.resteasy.reactive.server.core;

import java.util.ArrayList;
import java.util.List;
import org.jboss.resteasy.reactive.common.model.ResourceFeature;

public class Features {

    private final List<ResourceFeature> resourceFeatures = new ArrayList<>();

    public void addFeature(ResourceFeature resourceFeature) {
        resourceFeatures.add(resourceFeature);
    }

    public List<ResourceFeature> getResourceFeatures() {
        return resourceFeatures;
    }
}
