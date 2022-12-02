package io.quarkus.it.smallrye.config;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

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
