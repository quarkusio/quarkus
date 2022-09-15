package org.jboss.resteasy.reactive.server.model;

import jakarta.ws.rs.container.DynamicFeature;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
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
        for (ResourceDynamicFeature i : resourceDynamicFeatures) {
            if (i.getFactory() == null) {
                i.setFactory((BeanFactory<DynamicFeature>) factoryCreator.apply(i.getClassName()));
            }
        }
    }
}
