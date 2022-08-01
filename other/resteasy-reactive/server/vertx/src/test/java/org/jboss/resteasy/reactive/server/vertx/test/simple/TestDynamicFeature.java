package org.jboss.resteasy.reactive.server.vertx.test.simple;

import javax.ws.rs.Priorities;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

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
