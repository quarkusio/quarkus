package io.quarkus.resteasy.reactive.server.test.simple;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TestDynamicFeature implements DynamicFeature {

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        if (resourceInfo.getResourceClass().getName().equals(SimpleQuarkusRestResource.class.getName())
                && resourceInfo.getResourceMethod().getName().equals("dynamicFeatureFilters")) {
            context.register(DynamicFeatureRequestFilterWithLowPriority.class);
            context.register(new FeatureResponseFilter("feature-filter-response", "low-priority"), Priorities.USER - 1);
        }
    }
}
