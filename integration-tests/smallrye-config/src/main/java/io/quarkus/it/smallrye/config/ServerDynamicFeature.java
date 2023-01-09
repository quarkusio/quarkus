package io.quarkus.it.smallrye.config;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
public class ServerDynamicFeature implements DynamicFeature {

    @Inject
    @ConfigProperty(name = "server.info.version")
    Instance<String> version;

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext featureContext) {
        if (ServerResource.class.equals(resourceInfo.getResourceClass())
                && resourceInfo.getResourceMethod().getName().equals("info")) {
            featureContext.register(new ServerFilter(version.get()));
        }
    }
}
