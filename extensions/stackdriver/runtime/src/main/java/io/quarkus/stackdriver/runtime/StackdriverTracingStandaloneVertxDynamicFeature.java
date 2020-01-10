//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package io.quarkus.stackdriver.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

import io.quarkus.arc.Arc;
import io.quarkus.stackdriver.filter.ServerTracingFilter;

@Provider
@ApplicationScoped
public class StackdriverTracingStandaloneVertxDynamicFeature implements DynamicFeature {

    private SpanService spanService;

    public StackdriverTracingStandaloneVertxDynamicFeature() {
        this.spanService = Arc.container().instance(SpanService.class).get();
    }

    @Inject
    public StackdriverTracingStandaloneVertxDynamicFeature(SpanService spanService) {
        this.spanService = spanService;
    }

    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        context.register(new ServerTracingFilter(spanService));
    }
}
