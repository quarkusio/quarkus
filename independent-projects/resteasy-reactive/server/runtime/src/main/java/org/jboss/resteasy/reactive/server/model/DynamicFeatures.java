package org.jboss.resteasy.reactive.server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import jakarta.ws.rs.container.DynamicFeature;

import org.jboss.resteasy.reactive.common.model.ResourceDynamicFeature;
import org.jboss.resteasy.reactive.spi.BeanFactory;

/**
 * Container for {@link jakarta.ws.rs.container.DynamicFeature}
 */
public class DynamicFeatures {

    private final List<ResourceDynamicFeature> resourceDynamicFeatures = new ArrayList<>();

    public void addFeature(ResourceDynamicFeature resourceFeature) {
        resourceDynamicFeatures.add(resourceFeature);
    }

    public List<ResourceDynamicFeature> getResourceDynamicFeatures() {
        return resourceDynamicFeatures;
    }

    public void initializeDefaultFactories(Function<String, BeanFactory<?>> factoryCreator) {
        for (int i = 0; i < resourceDynamicFeatures.size(); i++) {
            ResourceDynamicFeature resourceFeature = resourceDynamicFeatures.get(i);
            if (resourceFeature.getFactory() == null) {
                resourceFeature.setFactory((BeanFactory<DynamicFeature>) factoryCreator.apply(resourceFeature.getClassName()));
            }
        }
    }
}
