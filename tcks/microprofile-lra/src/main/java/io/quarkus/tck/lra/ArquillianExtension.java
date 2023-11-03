package io.quarkus.tck.lra;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

public class ArquillianExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder extensionBuilder) {
        extensionBuilder.service(ApplicationArchiveProcessor.class, DeploymentProcessor.class);
        extensionBuilder.service(ResourceProvider.class, BaseURLProvider.class);
        // works but restarts coordinator with every test class
        extensionBuilder.observer(LRACoordinatorManager.class);
    }
}
