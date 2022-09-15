package io.quarkus.rest.client.reactive.provider;

import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestFilter;

@Provider
public class GlobalFeature implements Feature {

    public static boolean called;

    @Override
    public boolean configure(FeatureContext context) {
        context.register(FeatureInstalledFilter.class);
        return true;
    }

    public static class FeatureInstalledFilter implements ResteasyReactiveClientRequestFilter {

        @Override
        public void filter(ResteasyReactiveClientRequestContext requestContext) {
            called = true;
        }
    }
}
