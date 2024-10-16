package io.quarkus.resteasy.reactive.server.test.simple;

import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;

@Provider
public class TestFeature implements Feature {
    @Override
    public boolean configure(FeatureContext context) {
        context.register(FeatureMappedExceptionMapper.class);
        context.register(FeatureRequestFilterWithHighestPriority.class);
        context.register(FeatureRequestFilterWithNormalPriority.class);
        context.register(new FeatureResponseFilter("feature-filter-response", "normal-priority"));
        context.register(new FeatureResponseFilter("feature-filter-response", "high-priority"), Priorities.USER + 1);
        return true;
    }
}
