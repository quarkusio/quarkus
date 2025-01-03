package org.jboss.resteasy.reactive.server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import jakarta.ws.rs.core.Feature;

import org.jboss.resteasy.reactive.common.model.ResourceFeature;
import org.jboss.resteasy.reactive.spi.BeanFactory;

public class Features {

    private final List<ResourceFeature> resourceFeatures = new ArrayList<>();

    public void addFeature(ResourceFeature resourceFeature) {
        resourceFeatures.add(resourceFeature);
    }

    public List<ResourceFeature> getResourceFeatures() {
        return resourceFeatures;
    }

    public void initializeDefaultFactories(Function<String, BeanFactory<?>> factoryCreator) {
        for (int i = 0; i < resourceFeatures.size(); i++) {
            ResourceFeature resourceFeature = resourceFeatures.get(i);
            if (resourceFeature.getFactory() == null) {
                resourceFeature.setFactory((BeanFactory<Feature>) factoryCreator.apply(resourceFeature.getClassName()));
            }
        }
    }
}
